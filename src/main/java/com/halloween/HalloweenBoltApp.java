package com.javarush.halloween;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class HalloweenBoltApp extends SimpleTelegramBot {
    private AIService aiService = new AIService();
    private AppMode mode;

    private String imageType = "create_anime";

    private ArrayList<Path> imageList = new ArrayList<>();

    public HalloweenBoltApp(String token) {
        super(token);
    }


    //TODO: основний функціонал бота писатимемо тут
    public void onMessage() {

        if (mode == AppMode.CREATE) {
            imageMessage();
        } else if (mode == AppMode.EDIT) {
            editMessage();
        }else {
            String userText = getMessageText();

            sendTextMessage("Привіт!");
            sendTextMessage("Як справи, *друже*?");
            sendTextMessage("Ти написав: " + userText);
        }
    }

    public void startCommand() {
        mode = AppMode.MAIN;

        String currentChatId = getCurrentChatId();

        createUserDir(currentChatId);

        hideMainMenu();

        showMainMenu(
                "start", "🧟‍♂️ Головне меню бота",
                "image", "⚰️ Створюємо зображення",
                "edit", "🧙‍♂️ Змінюємо зображення",
                "merge", "🕷️ Об'єднуємо зображення",
                "party", "🎃 Фото для Halloween-вечірки",
                "video", "🎬☠️ Моторошне Halloween-відео з фото"
        );

        String main = loadMessage("main");

        sendPhotoMessage("main");
        sendTextMessage(main);
    }

    public void imageCommand() {
        mode = AppMode.CREATE;

        String messageText = loadMessage("create");
        sendPhotoMessage("create");

        sendTextButtonsCheckMessage(messageText, imageType,"create_anime", "\uD83D\uDC67 Аніме", "create_photo", "\uD83D\uDCF8 Фото");
    }

    public void imageButtonCallback() {
        imageType = getButtonKey();
        String messageText = loadMessage("create");

        Message message = getButtonMessage();

        updateMessage(message, messageText, imageType, "create_anime", "\uD83D\uDC67 Аніме", "create_photo", "\uD83D\uDCF8 Фото");
    }

    public void imageMessage() {
        String getMessageText = getMessageText();
        String userId = getCurrentChatId();

        Path photoPath = Path.of("users/" + userId + "/photo.jpg");

        String prompt = loadPrompt(imageType);

        aiService.createImage(prompt + getMessageText, photoPath);
        sendPhotoMessage(photoPath);
    }

    public void editCommand() {
        mode = AppMode.EDIT;

        String messageText = loadMessage("edit");
        Message message = getButtonMessage();

        sendPhotoMessage("edit");
        sendTextMessage(messageText);
    }

    public void editMessage() {
        String text = getMessageText();
        String userId = getCurrentChatId();

        Path photoPath = Path.of("users/" + userId + "/photo.jpg");

        if (!Files.exists(photoPath)) {
            sendTextMessage("Завантажте або створіть зображення");
            return;
        }

        String prompt = loadPrompt("edit");

        aiService.editImage(photoPath, prompt + text, photoPath);

        sendPhotoMessage(photoPath);
    }

    public void savePhoto() {
        var photo = getMessagePhotoList().getLast();
        String fileId = photo.getFileId();

        String userId = getCurrentChatId();
        Path photoPath = Path.of("users/" + userId + "/photo.jpg");

        downloadTelegramFile(fileId, photoPath);
        sendTextMessage("Фото готове до роботи");
    }

    public void mergeCommand() {
        mode = AppMode.MERGE;
        imageList.clear();

        String messageText = loadMessage("merge");

        sendPhotoMessage("merge");
        sendTextMessage(messageText);
        sendTextButtonsMessage("Вибери текст", "merge_join", "Лише об'єднати зображення",
                "merge_first", "Додати всіх на перше зображення",
                "merge_last", "Додати всіх на останнє зображення");
    }

    public void mergeAddPhoto() {
        var photo = getMessagePhotoList().getLast();
        var fileId = photo.getFileId();

        int count = imageList.size() + 1;

        String userId = getCurrentChatId();
        Path photoPath = Path.of("users/" + userId + "/photo"+ count + ".jpg");
        imageList.add(photoPath);
        int n = imageList.size();

        downloadTelegramFile(fileId, photoPath);

        sendTextMessage(n + " фото готове до роботи!");
    }

    public void onPhoto() {
        if (mode == AppMode.MERGE) {
            mergeAddPhoto();
        } else {
            savePhoto();
        }
    }

    public void mergeButtonCallback() {
        if (imageList.size() < 2) {
            sendTextMessage("Спочатку додай ваше фото");
            return;
        }

        String userId = getCurrentChatId();
        Path photoPath = Path.of("users/" + userId + "/result.jpg");

        String buttonKey = getButtonKey();

        String promt = loadPrompt(buttonKey);

        aiService.mergeImages(imageList, promt, photoPath);

        sendPhotoMessage(photoPath);
    }

    public void partyCommand() {
        mode = AppMode.PARTY;
        String messageText = loadMessage("party");
        sendPhotoMessage("party");

        sendTextButtonsMessage(messageText,
                "party_image1", " Місячне затемнення (перевертень)",
                "party_image2", " Прокляте дзеркало (вампір)",
                "party_image3", " Відьмине коло (дим і руни)",
                "party_image4", " Гниття часу (зомбі)",
                "party_image5", " Призов демона (демон)");
    }

    public void partyButtonCallback() {
        String chatId = getCurrentChatId();
        Path photoPath = Path.of("users/" + chatId + "/photo.jpg");
        Path resultPath = Path.of("users/" + chatId + "/result.jpg");

        if (Files.exists(root.resolve(photoPath)) == false) {
            sendTextMessage("Спочатку завантаж або створи зображення");
            return;
        }

        String buttonKey = getButtonKey();
        String promt = loadPrompt(buttonKey);

        aiService.editImage(photoPath, promt, resultPath);
        sendPhotoMessage(resultPath);
    }

    public void videoCommand() {
        mode = AppMode.VIDEO;
        String messageText = loadMessage("video");
        sendPhotoMessage("video");
        sendTextButtonsMessage(messageText,
                "video1", "🌕 Місячне затемнення (перевертень)",
                "video2", "🩸 Прокляте дзеркало (вампір)",
                "video3", "🧙‍♀️ Відьмине коло (дим і руни)",
                "video4", "🧟 Гниття часу (зомбі)",
                "video5", "😈 Пентаграма призову (демон)");
    }

    public void videoButtonCallback() {
        String chatId = getCurrentChatId();
        Path photoPath = Path.of("users/" + chatId + "/photo.jpg");
        Path resultPath = Path.of("users/" + chatId + "/video.mp4");

        if (!Files.exists(root.resolve(photoPath))) {
            sendTextMessage("Спочатку завантаж або створи зображення");
            return;
        }

        sendTextMessage("Генерація відео займе близько 20 секунд");

        String buttonKey = getButtonKey();
        String promt = loadPrompt(buttonKey);

        aiService.videoFromTextAndImage(photoPath, promt, resultPath);
        sendVideoMessage(resultPath);
    }

    @Override
    public void onInitialize() {
        addMessageTextHandler(this::onMessage);

        addButtonHandler("^create_.*", this::imageButtonCallback);
        addButtonHandler("^merge_.*", this::mergeButtonCallback);
        addButtonHandler("^party.*", this::partyButtonCallback);
        addButtonHandler("^video.*", this::videoButtonCallback);

        addCommandHandler("start", this::startCommand);
        addCommandHandler("image", this::imageCommand);
        addCommandHandler("edit", this::editCommand);
        addCommandHandler("merge", this::mergeCommand);
        addCommandHandler("party", this::partyCommand);
        addCommandHandler("video", this::videoCommand);

        addMessagePhotoHandler(this::onPhoto);
    }

    enum AppMode {
        MAIN,
        CREATE,
        EDIT,
        MERGE,
        PARTY,
        VIDEO
    }

    // Створюємо Telegram-бота
    public static void main(String[] args) throws TelegramApiException {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String telegramToken = env.get("TELEGRAM_TOKEN");

        var botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot(telegramToken, new HalloweenBoltApp(telegramToken));
    }
}