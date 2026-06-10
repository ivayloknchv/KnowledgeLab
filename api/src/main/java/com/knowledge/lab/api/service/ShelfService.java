package com.knowledge.lab.api.service;

import com.knowledge.lab.api.dto.request.ShelfRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.exception.ResourceNotFoundException;
import com.knowledge.lab.api.model.Shelf;
import com.knowledge.lab.api.model.User;
import com.knowledge.lab.api.repository.ShelfRepository;
import com.knowledge.lab.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelfService {

    private final ShelfRepository shelfRepository;
    private final UserRepository  userRepository;

    public Page<Responses.ShelfResponse> listPublicShelves(Pageable pageable) {
        return shelfRepository.findByIsPublicTrue(pageable).map(Responses.ShelfResponse::from);
    }

    public List<Responses.ShelfResponse> listSystemShelves() {
        return shelfRepository.findByOwnerIdIsNull()
                .stream().map(Responses.ShelfResponse::from).toList();
    }

    public List<Responses.ShelfResponse> listMyShelves(String userEmail) {
        String userId = resolveUserId(userEmail);
        return shelfRepository.findByOwnerId(userId)
                .stream().map(Responses.ShelfResponse::from).toList();
    }

    public Responses.ShelfResponse createShelf(ShelfRequests.CreateShelfRequest req, String userEmail) {
        String userId = resolveUserId(userEmail);
        if (shelfRepository.existsByOwnerIdAndName(userId, req.name())) {
            throw new IllegalStateException("You already have a shelf named '" + req.name() + "'");
        }
        Shelf shelf = Shelf.builder()
                .name(req.name())
                .description(req.description())
                .iconEmoji(req.iconEmoji())
                .ownerId(userId)
                .isPublic(req.isPublic())
                .build();
        return Responses.ShelfResponse.from(shelfRepository.save(shelf));
    }

    public Responses.ShelfResponse updateShelf(String shelfId, ShelfRequests.UpdateShelfRequest req, String userEmail) {
        String userId = resolveUserId(userEmail);
        Shelf shelf = findAndVerifyOwnership(shelfId, userId);
        if (req.name() != null)      shelf.setName(req.name());
        if (req.description() != null) shelf.setDescription(req.description());
        if (req.iconEmoji() != null) shelf.setIconEmoji(req.iconEmoji());
        if (req.isPublic() != null)  shelf.setPublic(req.isPublic());
        return Responses.ShelfResponse.from(shelfRepository.save(shelf));
    }

    public void deleteShelf(String shelfId, String userEmail) {
        String userId = resolveUserId(userEmail);
        Shelf shelf = findAndVerifyOwnership(shelfId, userId);
        shelfRepository.delete(shelf);
        log.info("Deleted shelf {} by user {}", shelfId, userEmail);
    }

    public Responses.ShelfResponse getShelf(String shelfId) {
        return Responses.ShelfResponse.from(findById(shelfId));
    }

    public void incrementContentCount(String shelfId) {
        shelfRepository.findById(shelfId).ifPresent(s -> {
            s.setContentCount(s.getContentCount() + 1);
            shelfRepository.save(s);
        });
    }

    public void decrementContentCount(String shelfId) {
        shelfRepository.findById(shelfId).ifPresent(s -> {
            s.setContentCount(Math.max(0, s.getContentCount() - 1));
            shelfRepository.save(s);
        });
    }

    public Shelf findById(String shelfId) {
        return shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf", shelfId));
    }

    private Shelf findAndVerifyOwnership(String shelfId, String userId) {
        Shelf shelf = findById(shelfId);
        if (!userId.equals(shelf.getOwnerId())) {
            throw new AccessDeniedException("You don't own this shelf");
        }
        return shelf;
    }

    private String resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
