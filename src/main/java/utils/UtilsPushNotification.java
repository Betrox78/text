package utils;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.Map;

/**
 * Singleton class for push notifications
 *
 * @author Hector Flores - hmflores95@gmail.com
 */
public class UtilsPushNotification {
    public static void send(String registrationToken, String title, String body) {
        send(registrationToken, title, body, null, null);
    }

    public static void send(String registrationToken, String title, String body, String image) {
        send(registrationToken, title, body, image, null);
    }

    public static void send(String fcmToken, String title, String body, String image, Map<String, String> data) {
        try {
            Notification.Builder notificationBuilder = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setImage(image);
            if (image != null) {
                notificationBuilder.setImage(image);
            }

            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notificationBuilder.build())
                    .setToken(fcmToken);

            if (data != null) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message: " + response);
        } catch (Exception e) {
            System.err.println("Error sending push notification: " + e.getMessage());
        }
    }
}
