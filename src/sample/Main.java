package sample;

import javafx.application.Application;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Timer;

import static sample.AppSettings.loadConfigFile;


public class Main extends Application
{
    private static Stage stage;
    private static Stage settingsStage, tagSettingsStage;
    private boolean flagFirstMinimise;

    private static java.awt.TrayIcon trayIcon;
    private static final String iconImageLoc
           = "http://nix.mrcur.ru:8080/static/b5ec8aab/images/headshot.png";
    private static final String settingsImageURL = "image/settings(small).png";
    private Timer notificationTimer = new Timer();

    @Override
    public void start(Stage primaryStage)
    {
        try
        {
            stage = primaryStage;
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("sample/main.fxml"));
            final int SCENE_WIDTH = 1200;
            final int SCENE_HEIGHT = 600;
            Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);

            //AppSettings.loadConfigFile();

            primaryStage.setTitle("Jenkins downloader");

            primaryStage.setScene(scene);

            primaryStage.setMaxHeight(SCENE_HEIGHT);
            primaryStage.setMaxWidth(SCENE_WIDTH + 5);

            primaryStage.setMinHeight(SCENE_HEIGHT);
            primaryStage.setMinWidth(700 + 5);

            primaryStage.setResizable(false);

            javax.swing.SwingUtilities.invokeLater(this::addAppToTray); //вызываем метод добавления иконки в трей



            stage.setOnCloseRequest(event -> {
                //System.exit(0);
                hideShowStage();   //по нажатию на крестик приложение сворачивается в трей
            });

            // выключаем автоматическое закрытие приложения если нет активных окон
            Platform.setImplicitExit(false);

            flagFirstMinimise = true;
            primaryStage.getIcons().add(new Image(iconImageLoc));

            primaryStage.show();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    private void addAppToTray() {
        try {
            java.awt.Toolkit.getDefaultToolkit();   // ensure awt toolkit is initialized.

            if (!java.awt.SystemTray.isSupported()) {   //проверка поддержки системой трея
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
            }

            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            URL imageLoc = new URL(iconImageLoc);               //ссылка на картинку для икноки

            java.awt.Image image = ImageIO.read(imageLoc).getScaledInstance(16, -1, 4);      // загружаем картинку для икноки
            trayIcon = new java.awt.TrayIcon(image);            // создаем иконку в трее

            trayIcon.addActionListener(event -> Platform.runLater(this::hideShowStage));                //событие при двойном клике по иконке в трее

            java.awt.MenuItem openItem = new java.awt.MenuItem("Jenkins Downloader");       //имя пункта контекстного меню иконки в трее
            openItem.addActionListener(event -> Platform.runLater(this::hideShowStage));              //событие при клике по пункту меню

            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);    //жирный шрифт для контекстного меню
            openItem.setFont(boldFont);

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(event -> {   //что бы закрыть прилоежние нужно нажать "Exit" в контекстном меню в трее
                notificationTimer.cancel();         //выключение таймера
                //Platform.exit();                  //команда на выход из приложения
                tray.remove(trayIcon);
                System.exit(0);              //командка "жесткого" закрытия приложения
            });

            final java.awt.PopupMenu popup = new java.awt.PopupMenu();  //создаем контекстное меню для приложения
            popup.add(openItem);                 //наполняем контекстное меню
            popup.addSeparator();                //наполняем контекстное меню
            popup.add(exitItem);                 //наполняем контекстное меню
            trayIcon.setPopupMenu(popup);        //добавляем контекстное меню для приложения

            tray.add(trayIcon); //добавляем иконку приложения в трей
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray: " + e);
        }
    }

    private void hideShowStage() {  //изменение состояния окна на противоположное
        if (stage != null) {

            if (flagFirstMinimise) {
                flagFirstMinimise = false;
                trayMessage("Jenkins Downloader is still running!");
            }

            if (stage.isShowing()) {
                //System.out.println("Stage hide");
                stage.hide();
            }
            else {
                //System.out.println("Stage show");
                stage.show();
                stage.toFront();
            }
        }
    }

    private void trayMessage (String text)
    {
            Runnable showMsg = () -> {
                try {
                    String caption = "Jenkins Downloader";  //заголовок сообщения
                    if (AppSettings.isShowNotifications())
                        trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO); //метод отображения сообщения в трее
                }
                catch (Exception e)
                {
                    System.out.println("Can't display tray message: " + e);
                }
            };
            Thread threadUpdateStatusOfJobs = new Thread(showMsg);
            threadUpdateStatusOfJobs.start();
    }

    public void openTagSettings()
    {
        try {

            Parent settingsRoot = FXMLLoader.load(getClass().getClassLoader().getResource("sample/tagSettings.fxml"));
            Scene settingsScene = new Scene(settingsRoot, 500, 600);

            tagSettingsStage = new Stage();
            tagSettingsStage.setTitle("TagSettingsController");
            tagSettingsStage.initModality(Modality.APPLICATION_MODAL);
            tagSettingsStage.initOwner(stage);

            tagSettingsStage.getIcons().add(new Image(settingsImageURL));

            tagSettingsStage.setScene(settingsScene);

//            tagSettingsStage.setMaxHeight(600);
//            tagSettingsStage.setMaxWidth(500);
//
//            tagSettingsStage.setMinWidth(600);
//            tagSettingsStage.setMinHeight(500);

            tagSettingsStage.setResizable(false);

            //settingsStage.setX(this.stage.getX() + 100);
            //settingsStage.setY(this.stage.getY() + 100);

            tagSettingsStage.show();
        }
        catch (Exception e)
        {
            System.out.println("(Main) Can't open settings: " + e);
        }
    }

    public void openSettings()
    {
        try {
            Parent settingsRoot = FXMLLoader.load(getClass().getClassLoader().getResource("sample/Settings.fxml"));
            Scene settingsScene = new Scene(settingsRoot, 400, 400);

            settingsStage = new Stage();
            settingsStage.setTitle("TagSettingsController");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(stage);

            settingsStage.getIcons().add(new Image(settingsImageURL));

            settingsStage.setScene(settingsScene);

            settingsStage.setResizable(false);

            //settingsStage.setX(this.stage.getX() + 100);
            //settingsStage.setY(this.stage.getY() + 100);

            settingsStage.show();
        }
        catch (Exception e)
        {
            System.out.println("(Main) Can't open settings: " + e);
        }
    }

    public static Stage getTagSettingsStage()
    {
        return tagSettingsStage;
    }

    public static Window getStage()
    {
        return stage;
    }

    public static Stage getSettingsStage()
    {
        return settingsStage;
    }

    public TrayIcon getTrayIcon ()
    {
        return trayIcon;
    }

}
