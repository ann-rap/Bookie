package com.program.bookie.models;

public class ReadingReminderNotification extends BaseNotification {
    private static final long serialVersionUID = 1L;

    private String bookTitle;
    private Integer bookId;
    private ReminderType reminderType;

    public enum ReminderType {
        SPECIFIC_BOOK,    // Przypomnienie o konkretnej książce
        GENERAL_READING   // Ogólne przypomnienie o czytaniu
    }

    public ReadingReminderNotification() {
        super();
    }

    // Konstruktor dla przypomnienia o konkretnej książce
    public ReadingReminderNotification(int userId, String bookTitle, int bookId) {
        super(userId,
                "Reading Reminder",
                "Don't forget to continue reading \"" + bookTitle + "\"!",
                bookId);
        this.bookTitle = bookTitle;
        this.bookId = bookId;
        this.reminderType = ReminderType.SPECIFIC_BOOK;
    }

    // Konstruktor dla ogólnego przypomnienia
    public ReadingReminderNotification(int userId) {
        super(userId,
                "Reading Reminder",
                "You haven't updated your reading progress in a while!",
                null);
        this.reminderType = ReminderType.GENERAL_READING;
    }

    @Override
    public String getIcon() {
        return "📚";
    }

    @Override
    public String getNotificationType() {
        return "READING_REMINDER";
    }

    @Override
    public void handleClick(Object controller) {
        if (controller instanceof com.program.bookie.app.controllers.MainController) {
            com.program.bookie.app.controllers.MainController mainController =
                    (com.program.bookie.app.controllers.MainController) controller;

            if (reminderType == ReminderType.SPECIFIC_BOOK && bookId != null) {
                // TODO:Otwórz szczegóły konkretnej książki
              //  mainController.openBookDetails(bookId);
                System.out.println("🔗 Opening book details for: " + bookTitle + " (Book ID: " + bookId + ")");
            } else {
                // Otwórz półki użytkownika
                mainController.onShelfClicked();
                System.out.println("🔗 Opening user's bookshelves");
            }
        }
    }

    // Gettery i settery
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    public ReminderType getReminderType() { return reminderType; }
    public void setReminderType(ReminderType reminderType) { this.reminderType = reminderType; }
}