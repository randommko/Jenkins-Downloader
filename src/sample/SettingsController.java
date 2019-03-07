package sample;

import com.sun.javafx.property.adapter.PropertyDescriptor;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
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
//        mainController = new MainController();
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
        main.openTagSettings();
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


        Stage mainStage = main.getSettingsStage();
        mainStage.close();
    }

    @FXML
    public void exitButton()
    {
        Stage mainStage = main.getSettingsStage();
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


    //    @FXML
//    private void onJobNameCheckBoxClick()
//    {
//        if (jobNameCheckBox.isSelected())
//            AppSettings.setShowColumnJobName(true);
//        else
//            AppSettings.setShowColumnJobName(false);
//    }
//
//    @FXML
//    private void onJobIDCheckBoxClick()
//    {
//        if (jobIDCheckBox.isSelected())
//            AppSettings.setShowColumnJobID(true);
//        else
//            AppSettings.setShowColumnJobID(false);
//    }
//
//    @FXML
//    private void onJobStatusCheckBoxClick()
//    {
//        if (jobStatusCheckBox.isSelected())
//            AppSettings.setShowColumnJobStatus(true);
//        else
//            AppSettings.setShowColumnJobStatus(false);
//    }
//
//    @FXML
//    private void onIsFileCheckBoxClick()
//    {
//        if (isFileCheckBox.isSelected())
//            AppSettings.setShowColumnIsFile(true);
//        else
//            AppSettings.setShowColumnIsFile(false);
//    }
//
//    @FXML
//    private void onTagNameCheckBoxClick()
//    {
//        if (tagNameCheckBox.isSelected())
//            AppSettings.setShowColumnTagName(true);
//        else
//            AppSettings.setShowColumnTagName(false);
//    }
}
