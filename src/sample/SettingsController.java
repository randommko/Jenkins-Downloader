package sample;

import com.sun.javafx.property.adapter.PropertyDescriptor;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;

import static sample.AppSettings.loadConfigFile;

public class SettingsController
{
    @FXML
    private TextField serverAddressTextField, downloadPathTextField;
    @FXML
    private CheckBox jobNameCheckBox, jobIDCheckBox, jobStatusCheckBox, isFileCheckBox, tagNameCheckBox;
    @FXML
    private CheckBox showNotificationsCheckBox, showAllJobsCheckBox, autoUpdateCheckBox, lastChangeCheckBox;

    private Main main;
    private static Stage tagSettingsStage;

    public static final int TAG_SCENE_WIDTH = 800;
    public static final int TAG_SCENE_HEIGHT = 400;

    private static final String settingsImageURL = "image/settings(small).png";

    @FXML
    private  void initialize()
    {
        loadConfigFile();

        setServerAddress(AppSettings.getServerAddress());
        setSavePath(AppSettings.getSavePath());

        jobNameCheckBox.setSelected(AppSettings.showColumnJobName());
        jobIDCheckBox.setSelected(AppSettings.showColumnJobID());
        jobStatusCheckBox.setSelected(AppSettings.showColumnJobStatus());
        isFileCheckBox.setSelected(AppSettings.showColumnIsFile());
        tagNameCheckBox.setSelected(AppSettings.showColumnTagName());
        lastChangeCheckBox.setSelected(AppSettings.showColumnLastTimeUpdate());

        showNotificationsCheckBox.setSelected(AppSettings.isShowNotifications());
        showAllJobsCheckBox.setSelected(AppSettings.isShowAllJobs());
        autoUpdateCheckBox.setSelected(AppSettings.isAutoUpdate());

        main = new Main();

    }

    @FXML
    public void setDirectory ()            //нажатие на кнопку "Выбрать директорию..."
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберете папку для сохранения");

        try {
            File selectedDirectory = directoryChooser.showDialog(main.getStage());
            downloadPathTextField.setText(selectedDirectory.getAbsolutePath());
        }
        catch (NullPointerException e) {
            System.out.println("(SettingsController) No directory: " + e);
        }
    }

    @FXML
    public void onSettingsClick()
    {
        openTagSettings();
    }

    @FXML
    public void saveButton()
    {
        AppSettings.setServerAddress(serverAddressTextField.getText());
        AppSettings.setSavePath(downloadPathTextField.getText());

        AppSettings.setShowColumnTagName(tagNameCheckBox.isSelected());
        AppSettings.setShowColumnIsFile(isFileCheckBox.isSelected());
        AppSettings.setShowColumnJobStatus(jobStatusCheckBox.isSelected());
        AppSettings.setShowColumnJobName(jobNameCheckBox.isSelected());
        AppSettings.setShowColumnJobID(jobIDCheckBox.isSelected());
        AppSettings.setShowColTimeLastUpdate(lastChangeCheckBox.isSelected());

        AppSettings.setShowNotifications(showNotificationsCheckBox.isSelected());
        AppSettings.setShowAllJobs(showAllJobsCheckBox.isSelected());
        AppSettings.setAutoUpdate(autoUpdateCheckBox.isSelected());


        Stage mainStage = MainController.getSettingsStage();
        mainStage.close();
    }

    @FXML
    public void exitButton()
    {
        Stage mainStage = MainController.getSettingsStage();
        mainStage.close();
    }

    private void setServerAddress(String addr)
    {
        Runnable setAddr = () -> {
            serverAddressTextField.clear();
            serverAddressTextField.setText(addr);
        };
        Thread thread = new Thread(setAddr);
        thread.start();
    }

    private void setSavePath(String path)
    {
        Runnable setPath = () -> {
            downloadPathTextField.clear();
            downloadPathTextField.setText(path);
        };
        Thread thread = new Thread(setPath);
        thread.start();
    }

    private void openTagSettings()   //открите настроек тэгов
    {
        try {

            Parent settingsRoot = FXMLLoader.load(getClass().getClassLoader().getResource("sample/tagSettings.fxml"));
            Scene settingsScene = new Scene(settingsRoot, TAG_SCENE_WIDTH, TAG_SCENE_HEIGHT);

            tagSettingsStage = new Stage();
            tagSettingsStage.setTitle("TagSettingsController");
            tagSettingsStage.initModality(Modality.APPLICATION_MODAL);
            tagSettingsStage.initOwner(MainController.getSettingsStage());

            tagSettingsStage.getIcons().add(new Image(settingsImageURL));

            tagSettingsStage.setScene(settingsScene);

            tagSettingsStage.setMaxHeight(TAG_SCENE_HEIGHT + 300);
            tagSettingsStage.setMaxWidth(TAG_SCENE_WIDTH + 300);

            tagSettingsStage.setMinHeight(TAG_SCENE_HEIGHT - 200);
            tagSettingsStage.setMinWidth(TAG_SCENE_WIDTH - 300);

            tagSettingsStage.setResizable(true);

            tagSettingsStage.show();
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
}
