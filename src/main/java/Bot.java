import java.util.Set;
import com.twilio.Twilio;
import java.util.HashSet;
import com.twilio.type.PhoneNumber;
import com.twilio.rest.api.v2010.account.Message;
import java.io.IOException;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.util.Properties;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Bot {
    private static void sendTextMessage(String authToken, String accountSid, String serviceSid,
                                        Set<String> phoneNumbers) {
        Twilio.init(accountSid, authToken);

        Set<String> usedPhoneNumbers = new HashSet<>();

        for (String number : phoneNumbers) {
            PhoneNumber receiver = new PhoneNumber(number);

            String messageText = "Finca El Suelo is on Tim's website!";

            Message message = Message.creator(receiver, serviceSid, messageText)
                                     .create();

            usedPhoneNumbers.add(number);

            String messageSid = message.getSid();

            System.out.printf("Message sent to %s: %s%n", number, messageSid);
        } //end for

        phoneNumbers.removeAll(usedPhoneNumbers);
    } //sendTextMessage

    private static void checkForRelease(String authToken, String accountSid, String serviceSid,
                                        Set<String> phoneNumbers) throws IOException {
        String url = "https://timwendelboe.no/product-category/coffee/filter-coffee/";

        Document document = Jsoup.connect(url)
                                 .get();

        String anchorSelector = "a";

        Elements elements = document.select(anchorSelector);

        for (Element element : elements) {
            String text = element.text();

            text = text.strip();

            text = text.toUpperCase();

            String regex = ".*FINCA.*EL.*SUELO.*";

            if (text.matches(regex)) {
                Bot.sendTextMessage(authToken, accountSid, serviceSid, phoneNumbers);

                break;
            } //end if
        } //end for
    } //checkForRelease

    public static void main(String[] args) {
        String pathString = "src/main/resources/application.properties";

        Path path = Path.of(pathString);

        BufferedReader reader;

        Properties properties;

        try {
            reader = Files.newBufferedReader(path);

            properties = new Properties();

            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();

            return;
        } //end try catch

        String accountSid = properties.getProperty("account_sid");

        if (accountSid == null) {
            throw new IllegalStateException();
        } //end if

        String authToken = properties.getProperty("auth_token");

        if (authToken == null) {
            throw new IllegalStateException();
        } //end if

        String serviceSid = properties.getProperty("service_sid");

        if (serviceSid == null) {
            throw new IllegalStateException();
        } //end if

        String phoneNumber = properties.getProperty("phone_number");

        if (phoneNumber == null) {
            throw new IllegalStateException();
        } //end if

        Set<String> phoneNumbers = new HashSet<>();

        phoneNumbers.add(phoneNumber);

        Runnable runnable = () -> {
            System.out.println("Checking for Finca El Suelo...");

            try {
                Bot.checkForRelease(authToken, accountSid, serviceSid, phoneNumbers);
            } catch (IOException e) {
                e.printStackTrace();
            } //end try catch
        };

        long initialDelay = 0;

        long delay = 5;

        Executors.newSingleThreadScheduledExecutor()
                 .scheduleAtFixedRate(runnable, initialDelay, delay, TimeUnit.MINUTES);
    } //main
}