package com.knowledge.lab.api.service;

import com.knowledge.lab.api.dto.request.ReadingListRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.exception.ResourceNotFoundException;
import com.knowledge.lab.api.model.ReadingList;
import com.knowledge.lab.api.model.User;
import com.knowledge.lab.api.repository.ReadingListRepository;
import com.knowledge.lab.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingListService {

    private final ReadingListRepository readingListRepository;
    private final UserRepository        userRepository;
    private final ContentService        contentService;

    public Responses.ReadingListResponse create(ReadingListRequests.CreateReadingListRequest req,
                                                String userEmail) {
        String userId = resolveUserId(userEmail);
        if (readingListRepository.existsByOwnerIdAndName(userId, req.name())) {
            throw new IllegalStateException("Reading list named '" + req.name() + "' already exists");
        }
        ReadingList list = ReadingList.builder()
                .ownerId(userId)
                .name(req.name())
                .description(req.description())
                .isPublic(req.isPublic())
                .build();
        return Responses.ReadingListResponse.from(readingListRepository.save(list));
    }

    public List<Responses.ReadingListResponse> listMine(String userEmail) {
        String userId = resolveUserId(userEmail);
        return readingListRepository.findByOwnerId(userId)
                .stream().map(Responses.ReadingListResponse::from).toList();
    }

    public Responses.ReadingListDetailResponse getDetail(String listId, String requesterEmail) {
        String requesterId = resolveUserId(requesterEmail);
        ReadingList list = findById(listId);
        if (!list.isPublic() && !list.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("This reading list is private");
        }
        return Responses.ReadingListDetailResponse.from(list);
    }

    public void delete(String listId, String userEmail) {
        String userId = resolveUserId(userEmail);
        ReadingList list = findAndVerifyOwnership(listId, userId);
        readingListRepository.delete(list);
    }

    public Responses.ReadingListDetailResponse addBookmark(String listId,
                                                           ReadingListRequests.AddBookmarkRequest req,
                                                           String userEmail) {
        String userId = resolveUserId(userEmail);
        ReadingList list = findAndVerifyOwnership(listId, userId);
        contentService.findById(req.contentId());

        boolean alreadyAdded = list.getEntries().stream()
                .anyMatch(e -> e.getContentId().equals(req.contentId()));
        if (alreadyAdded) {
            throw new IllegalStateException("Content already in this reading list");
        }

        ReadingList.BookmarkEntry entry = ReadingList.BookmarkEntry.builder()
                .contentId(req.contentId())
                .note(req.note())
                .read(false)
                .addedAt(Instant.now())
                .build();

        list.getEntries().add(entry);
        return Responses.ReadingListDetailResponse.from(readingListRepository.save(list));
    }

    public Responses.ReadingListDetailResponse updateBookmark(String listId,
                                                              String contentId,
                                                              ReadingListRequests.UpdateBookmarkRequest req,
                                                              String userEmail) {
        String userId = resolveUserId(userEmail);
        ReadingList list = findAndVerifyOwnership(listId, userId);

        list.getEntries().stream()
                .filter(e -> e.getContentId().equals(contentId))
                .findFirst()
                .ifPresent(entry -> {
                    if (req.note() != null)  entry.setNote(req.note());
                    if (req.read() != null) {
                        entry.setRead(req.read());
                        if (req.read()) entry.setReadAt(Instant.now());
                    }
                });

        return Responses.ReadingListDetailResponse.from(readingListRepository.save(list));
    }

    public Responses.ReadingListDetailResponse removeBookmark(String listId,
                                                              String contentId,
                                                              String userEmail) {
        String userId = resolveUserId(userEmail);
        ReadingList list = findAndVerifyOwnership(listId, userId);
        list.getEntries().removeIf(e -> e.getContentId().equals(contentId));
        return Responses.ReadingListDetailResponse.from(readingListRepository.save(list));
    }

    private ReadingList findById(String listId) {
        return readingListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("ReadingList", listId));
    }

    private ReadingList findAndVerifyOwnership(String listId, String userId) {
        ReadingList list = findById(listId);
        if (!list.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You don't own this reading list");
        }
        return list;
    }

    private String resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
