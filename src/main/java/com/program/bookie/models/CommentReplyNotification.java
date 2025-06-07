package com.program.bookie.models;

public class CommentReplyNotification extends BaseNotification {
    private static final long serialVersionUID = 1L;

    private String commenterName;
    private String bookTitle;
    private int reviewId;

    public CommentReplyNotification() {
        super();
    }

    public CommentReplyNotification(int userId, String commenterName, String bookTitle, int reviewId) {
        super(userId,
                "New Comment",
                commenterName + " replied to your review of \"" + bookTitle + "\"",
                reviewId);
        this.commenterName = commenterName;
        this.bookTitle = bookTitle;
        this.reviewId = reviewId;
    }

    @Override
    public String getIcon() {
        return "💬";
    }

    @Override
    public String getNotificationType() {
        return "COMMENT_REPLY";
    }

    @Override
    public void handleClick(Object controller) {
        // Otwórz szczegóły książki z konkretną recenzją
        if (controller instanceof com.program.bookie.app.controllers.MainController) {
            //TODO:
           // com.program.bookie.app.controllers.MainController mainController =
          //          (com.program.bookie.app.controllers.MainController) controller;
         //   mainController.openBookFromReview(reviewId);
        }
        System.out.println("🔗 Opening book review for: " + bookTitle + " (Review ID: " + reviewId + ")");
    }

    // Gettery dla dodatkowych pól
    public String getCommenterName() { return commenterName; }
    public void setCommenterName(String commenterName) { this.commenterName = commenterName; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
}