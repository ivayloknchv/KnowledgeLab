# Endpoints

## Authentication
- POST /auth/register
- POST /auth/login
- POST /auth/logout

## Users
- GET /users
- GET /users/{user_id}
- PUT /users/{user_id}
- DELETE /users/{user_id}

## Bookmarks
- GET /bookmarks/{user_id}
- GET /bookmarks/{user_id}/{bookmark_id}
- POST /bookmarks
- PUT /bookmarks/{bookmark_id}
- DELETE /bookmarks/{user_id}/{bookmark_id}

## Documents
- GET /documents
- GET /documents/{document_id}
- POST /documents
- PUT /documents
- DELETE /documents/{document_id}

## Documents - Likes
- GET /documents/{document_id}/likes
- POST /documents/{document_id}/like
- DELETE /documents/{document_id}/like

## Documents - Comments
- GET /documents/{document_id}/comments
- POST /documents/{document_id}/comments
- POST /documents/{document_id}/comments/{parent_comment_id}
- PUT /documents/{document_id}/comments
- DELETE /documents/{document_id}/comments/{comment_id}

## Recommendations
- GET /recommendations/documents
- GET /recommendations/bookmarks
