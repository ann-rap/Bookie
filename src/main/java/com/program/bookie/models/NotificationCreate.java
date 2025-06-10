package com.program.bookie.models;

public class NotificationCreate {

    public static CommentReplyNotification createCommentReply(int userId, String bookTitle, String commenterName, int reviewId) {
        return new CommentReplyNotification(userId, commenterName, bookTitle, reviewId);
    }
    public static CommentReplyNotification createCommentReply(int userId, String bookTitle, String commenterName, int reviewId, int bookId) {
        return new CommentReplyNotification(userId, commenterName, bookTitle, reviewId, bookId);
    }


}