package sample;

import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.Console;


public class Main extends Application
{
    private final int SCENE_WIDTH = 1000;
    private final int SCENE_HEIGHT = 600;
    private Parent root;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        //TODO: добавить сворачивание в трей и показ уведомлений о новых билдах
        try
        {

            root = FXMLLoader.load(getClass().getClassLoader().getResource("sample/sample.fxml"));

            primaryStage.setTitle("Jenkins downloader");

            primaryStage.setScene(new Scene(root, SCENE_WIDTH, SCENE_HEIGHT));

            primaryStage.setMaxHeight(600);
            primaryStage.setMaxWidth(1000);

            primaryStage.setMinWidth(750);
            primaryStage.setMinHeight(300);

            primaryStage.setResizable(true);

            stage = primaryStage;

            stage.setOnCloseRequest(event -> {
                System.exit(0);
            });


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

    public int getSCENE_WIDTH()
    {
        return SCENE_WIDTH;
    }

    public int getSCENE_HEIGHT()
    {
        return SCENE_HEIGHT;
    }

    public Window getStage()
    {
        return stage;
    }
}
