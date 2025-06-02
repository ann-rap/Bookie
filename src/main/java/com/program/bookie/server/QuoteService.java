package com.program.bookie.server;

import com.program.bookie.models.Quote;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuoteService {
    private static final String QUOTES_FILE_PATH = "quotes.txt";
    private List<Quote> quotes;
    private Random random;

    public QuoteService() {
        this.quotes = new ArrayList<>();
        this.random = new Random();
        loadQuotesFromFile();
    }

    /**
     * Ładuje cytaty z pliku tekstowego.
     * Format pliku: każda linia to cytat w formacie "tekst cytatu" - Autor
     * lub po prostu "tekst cytatu" (bez autora)
     */
    private void loadQuotesFromFile() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(QUOTES_FILE_PATH), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { // Ignoruj puste linie i komentarze
                    Quote quote = parseQuoteLine(line);
                    if (quote != null) {
                        quotes.add(quote);
                    }
                }
            }

            System.out.println("Załadowano " + quotes.size() + " cytatów z pliku " + QUOTES_FILE_PATH);

        } catch (FileNotFoundException e) {
            System.err.println("Plik z cytatami nie został znaleziony: " + QUOTES_FILE_PATH);
            loadDefaultQuotes();
        } catch (IOException e) {
            System.err.println("Błąd podczas wczytywania cytatów: " + e.getMessage());
            loadDefaultQuotes();
        }

        // Jeśli nie udało się załadować żadnych cytatów, załaduj domyślne
        if (quotes.isEmpty()) {
            loadDefaultQuotes();
        }
    }

    /**
     * Parsuje linię z pliku i tworzy obiekt Quote
     */
    private Quote parseQuoteLine(String line) {
        try {
            // Usuń cudzysłowy na początku i końcu jeśli są
            if (line.startsWith("\"") && line.contains("\"")) {
                int lastQuoteIndex = line.lastIndexOf("\"");
                if (lastQuoteIndex > 0) {
                    String text = line.substring(1, lastQuoteIndex);
                    String remainder = line.substring(lastQuoteIndex + 1).trim();

                    String author = null;
                    if (remainder.startsWith("-")) {
                        author = remainder.substring(1).trim();
                    }

                    return new Quote(text, author);
                }
            }

            // Jeśli nie ma cudzysłowów, sprawdź czy jest " - " oddzielające autora
            if (line.contains(" - ")) {
                int dashIndex = line.lastIndexOf(" - ");
                String text = line.substring(0, dashIndex).trim();
                String author = line.substring(dashIndex + 3).trim();
                return new Quote(text, author);
            }

            // Jeśli nic nie pasuje, cały tekst to cytat bez autora
            return new Quote(line, null);

        } catch (Exception e) {
            System.err.println("Błąd podczas parsowania linii: " + line + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Ładuje domyślne cytaty do pamięci
     */
    private void loadDefaultQuotes() {
        quotes.clear();
        quotes.add(new Quote("A reader lives a thousand lives before he dies. The man who never reads lives only one.", "George R.R. Martin"));
        quotes.add(new Quote("Books are a uniquely portable magic.", "Stephen King"));
        quotes.add(new Quote("The more that you read, the more things you will know. The more that you learn, the more places you'll go.", "Dr. Seuss"));
        quotes.add(new Quote("A book is a dream that you hold in your hand.", "Neil Gaiman"));
        quotes.add(new Quote("Reading is escape, and the opposite of escape; it's a way to make contact with reality after a day of making things up.", "Nora Ephron"));
        quotes.add(new Quote("Books fall open, you fall in.", "David T.W. McCord"));
        quotes.add(new Quote("A room without books is like a body without a soul.", "Marcus Tullius Cicero"));
        quotes.add(new Quote("So many books, so little time.", "Frank Zappa"));

        System.out.println("Załadowano " + quotes.size() + " domyślnych cytatów");
    }

    /**
     * Zwraca losowy cytat
     */
    public Quote getRandomQuote() {
        if (quotes.isEmpty()) {
            return new Quote("Welcome to Bookie - your personal reading companion!", "Bookie Team");
        }

        int randomIndex = random.nextInt(quotes.size());
        return quotes.get(randomIndex);
    }

    /**
     * Przeładowuje cytaty z pliku (przydatne jeśli plik został zmieniony)
     */
    public void reloadQuotes() {
        quotes.clear();
        loadQuotesFromFile();
    }

    /**
     * Zwraca liczbę załadowanych cytatów
     */
    public int getQuotesCount() {
        return quotes.size();
    }
}