package sample;

import javafx.application.Application;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.Console;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.sun.javafx.scene.control.skin.Utils.getResource;


public class Main extends Application
{
    private final int SCENE_WIDTH = 1000;
    private final int SCENE_HEIGHT = 600;
    private Stage stage;
    private boolean flagFirtsMinimaise;


    private java.awt.TrayIcon trayIcon;
    private static final String iconImageLoc
//            "http://icons.iconarchive.com/icons/scafer31000/bubble-circle-3/16/GameCenter-icon.png";
           = "http://nix.mrcur.ru:8080/static/b5ec8aab/images/headshot.png";
    private Timer notificationTimer = new Timer();

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        //TODO: добавить сворачивание в трей и показ уведомлений о новых билдах
        try
        {
            this.stage = primaryStage;
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("sample/sample.fxml"));

            primaryStage.setTitle("Jenkins downloader");
            Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            primaryStage.setScene(scene);

            primaryStage.setMaxHeight(600);
            primaryStage.setMaxWidth(1000);

            primaryStage.setMinWidth(750);
            primaryStage.setMinHeight(300);

            primaryStage.setResizable(true);

            javax.swing.SwingUtilities.invokeLater(this::addAppToTray); //вызываем метод добавления иконки в трей

            stage.setOnCloseRequest(event -> {
                //System.exit(0);
                hideShowStage();   //по нажатию на крестик приложение сворачивается в трей
            });

            // выключаем автоматическое закрытие приложения если нет активных окон
            Platform.setImplicitExit(false);

            flagFirtsMinimaise = true;
            //trayMessage("Hello! This is Jenkins Downloader!");
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

    public Window getStage()
    {
        return stage;
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

            if (flagFirtsMinimaise) {
                flagFirtsMinimaise = false;
                trayMessage("Jenkins Downloader is still running!");
            }

            if (stage.isShowing()) {
                System.out.println("Stage hide");
                stage.hide();
            }
            else {
                System.out.println("Stage show");
                stage.show();
                stage.toFront();
            }
        }
    }

    public void trayMessage (String text)
    {
            Runnable showMsg = () -> {
                try {
                    String caption = "Jenkins Downloader";  //заголовок сообщения
                    //System.out.println("text to tray: " + text);
                    //System.out.println("caption: " + caption);
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
}
