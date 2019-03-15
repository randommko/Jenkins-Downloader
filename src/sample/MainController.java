package sample;


import java.text.DecimalFormat;
import java.util.Iterator;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.*;
import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;




public class MainController
{
    @FXML
    private volatile Label statusLabel;
    @FXML
    private TextArea logTextArea;

    @FXML
    private TableColumn<Job, String> jobNameCol;
    @FXML
    private TableColumn<Job, Job.JobStatusListing> jobStatusCol;
    @FXML
    private TableColumn<Job, Integer> IDCol;
    @FXML
    private TableColumn<Job, String> isFileCol;
    @FXML
    private TableColumn<Job, String> tagCol;
    @FXML
    private TableColumn<Job, String> lastChangeCol;
    @FXML
    private TableView<Job> jobsTable;

    @FXML
    private Button refreshButton, downloadButton, hideLogButton, settingsButton, helpButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private AnchorPane logAnchorPane;


    private static Stage settingsStage, helpStage;
    private Main main;
    private static JenkinsJobs jobsForMainForm;
    private static JenkinsJobs allFoundJobs;
    public enum ClientStatus {Disconnected, Connected, Downloading, Extracting, Connecting, Updating, _lastStatus}
    private ClientStatus lastStatus, actualStatus;
    private static final String settingsImageURL = "image/settings(small).png";
    private static final String helpImageURL = "image/help.png";
    private boolean hideLog;


    @FXML
    private void initialize() //метод в котором выполняется код при запуске приложения
    {
        jobsForMainForm = new JenkinsJobs();
        allFoundJobs = new JenkinsJobs();

        AppSettings.loadConfigFile();

        initWindow();

        setStatus(ClientStatus.Disconnected);

        connectToServer();

        Thread threadUpdateStatusOfJobs = new Thread(endlessUpdateStatusOfJobs());
        threadUpdateStatusOfJobs.start();

    }

    private Runnable endlessUpdateStatusOfJobs()
    {
          Runnable updateStatus;
          updateStatus  = () -> {
            String out;
            do {
                if (AppSettings.isAutoUpdate())
                {
                    try {
                        out = allFoundJobs.refreshStatusOfAllJobs(AppSettings.getServerAddress());   //обновляем статусы всех работ. Возврашает строку с навзаниями работ у которых статус изменился

                        if (!out.equals("No jobs has been updated")) {
                            writeToLog(out);
                            trayMessage(out);
                        }
                    }
                    catch (Exception e) {
                        System.out.println("(MainController) (endlessUpdateStatusOfJobs) Error on job status updating: " + e);
                        if (actualStatus == ClientStatus.Connected)
                            setStatus(ClientStatus.Disconnected);
                    }
                }
                sleep(1);
            } while (true);
        };
        return updateStatus;
    }

    private void sleep(int timeout)
    {
        try
        {
            TimeUnit.SECONDS.sleep(timeout);
        }
        catch (Exception err)
        {
            System.out.println("(MainController) (sleep) Can't call sleep method: " + err);
        }
    }

    @FXML
    private void connectToServer()              //нажатие кнопки "Refresh"
    {
        setStatus(ClientStatus.Connecting);
        connect();
    }

    private boolean isJenkins(String address)
    {
        try
        {
            Document document = Jsoup.connect(address).get(); //получаем копию страницы в виде документа

            Elements elements = document.select("title");
            Iterator iterator = elements.iterator();
            String serverName = "";

            while ( iterator.hasNext() )
            {
                Element element = (Element) iterator.next();
                serverName = element.attr("Jenkins");
            }

            return serverName.equals("");
        }
        catch (IOException e)
        {
            writeToLog("Enter valid jenkins server address: \"http://[address]:[port]\"");
            setStatus(ClientStatus.Disconnected);
            trayMessage("Server not found");
            return false;
        }
        catch (IllegalArgumentException err)
        {
            writeToLog("Enter correct server address");
            setStatus(ClientStatus.Disconnected);
            return false;
        }
    }

    private void connect()
    {
        jobsForMainForm.clear();
        allFoundJobs.clear();

        String serverAddress = AppSettings.getServerAddress();

        Runnable runnableGetJobList = () -> {

            if ( isJenkins(serverAddress) ) {
                setStatus(ClientStatus.Updating);
                trayMessage("Updating job list");

                refreshingAllJobsStatus(serverAddress);
            }
            else
                setStatus(ClientStatus.Disconnected);
        };

        Thread threadGetJobList = new Thread(runnableGetJobList);
        threadGetJobList.start();
    }

    private void refreshingAllJobsStatus(String serverAddress)
    {
        try {
            jobsForMainForm.getJobListFromServer(serverAddress);    //Получение спика работ
            allFoundJobs = new JenkinsJobs(jobsForMainForm);

            if (!AppSettings.isShowAllJobs())
            {
                Iterator iterator = jobsForMainForm.getListOfJobs().iterator();
                while (iterator.hasNext())
                {
                    Job job = (Job) iterator.next();
                    if ( !job.isFile() )
                        iterator.remove();
                }
            }
            writeToLog("Job list has been updated.");
            setStatus(ClientStatus.Connected);
        }
        catch (IllegalArgumentException err)
        {
            writeToLog("Incorrect server address");
            System.out.println("MainController) (refreshingAllJobsStatus) Incorrect server address: " + err);
            setStatus(ClientStatus.Disconnected);
        }
        catch (Exception error)
        {
            System.out.println("(MainController) (refreshingAllJobsStatus) Unknown error: " + error);
            setStatus(ClientStatus.Disconnected);
        }
    }

    @FXML
    private void downloadJobButton ()           //Нажатие на кнопку "Download" и начало скачивания в новом потоке
    {
        try
        {
            Job job = getSelectedJob();
            startJobDownload(job);
        }
        catch (NullPointerException e)
        {
            writeToLog("Select job to download");
        }
    }

    @FXML
    public void onSettingsClick()   //Открытие настроек
    {
        openSettings();

        //TODO: проверить, можно ли убрать этот костыль
        Runnable runnable = () -> {
                while (settingsStage.isShowing())
                    sleep(1);

                loadTableConfig();
            };
        Thread thread = new Thread(runnable);   //необходимо для загузрки актуального конфига после закрытия окна настроек
        thread.start();

    }

    public static Stage getSettingsStage()
    {
        return settingsStage;
    }

    private void initWindow()
    {
        main = new Main();
        hideLog = false;


        try {
            Image settings = new Image(settingsImageURL);
            settingsButton.setGraphic(new ImageView(settings));
        }
        catch (Exception e)
        {
            System.out.println("(MainController) Can't set image for settings button: " + e);
        }

        logTextArea.setEditable(false);

        configTable();
    }

    private void loadTableConfig()
    {
        Platform.runLater(
                () -> {
                    jobNameCol.setVisible(AppSettings.showColumnJobName());
                    IDCol.setVisible(AppSettings.showColumnJobID());
                    jobStatusCol.setVisible(AppSettings.showColumnJobStatus());
                    isFileCol.setVisible(AppSettings.showColumnIsFile());
                    tagCol.setVisible(AppSettings.showColumnTagName());
                    lastChangeCol.setVisible(AppSettings.showColumnLastTimeUpdate());

                    jobNameCol.setPrefWidth(AppSettings.getWidthColumnJobName());
                    IDCol.setPrefWidth(AppSettings.getWidthColumnJobID());
                    jobStatusCol.setPrefWidth(AppSettings.getWidthColumnJobStatus());
                    isFileCol.setPrefWidth(AppSettings.getWidthColumnIsFile());
                    tagCol.setPrefWidth(AppSettings.getWidthColumnTagName());
                    lastChangeCol.setPrefWidth(AppSettings.getWidthColumnTimeLastUpdate());
                }
        );
    }

    private void configTable()
    {
        jobsTable.setMinWidth(700);
        jobsTable.setMaxWidth(700);
        jobsTable.setEditable(false);

        loadTableConfig();

        jobNameCol.setEditable(false);
        IDCol.setEditable(false);
        jobStatusCol.setEditable(false);
        isFileCol.setEditable(false);
        tagCol.setEditable(false);
        lastChangeCol.setEditable(false);

        jobNameCol.setCellValueFactory(new PropertyValueFactory<>("jobName"));
        IDCol.setCellValueFactory(new PropertyValueFactory<>("jobID"));
        jobStatusCol.setCellValueFactory(new PropertyValueFactory<>("jobStatus"));
        isFileCol.setCellValueFactory(new PropertyValueFactory<>("fileForTable"));
        tagCol.setCellValueFactory(new PropertyValueFactory<>("visibleName"));
        lastChangeCol.setCellValueFactory(new PropertyValueFactory<>("lastChange"));


        jobsTable.setRowFactory( tv ->
        {
            TableRow<Job> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    Job job = row.getItem();
                    if (job.isFile())
                    {
                        if (job.getJobStatus() != Job.JobStatusListing.Впроцессе)
                            startJobDownload(job);
                        else
                            writeToLog(job.getJobName() + " (#" + job.getJobID() + ")" + " is in processing");
                    }
                    else
                    {
                        if (job.getJobStatus() != Job.JobStatusListing.Впроцессе)
                            //startJob(job);
                            writeToLog("Can't starts jobs. Only downloading!");
                        else
                            writeToLog(job.getJobName() + " (#" + job.getJobID() + ")" + " is in processing");
                    }
                }
            });
            return row ;
        });

        changeColumnWidthListener(jobNameCol);
        changeColumnWidthListener(IDCol);
        changeColumnWidthListener(jobStatusCol);
        changeColumnWidthListener(isFileCol);
        changeColumnWidthListener(tagCol);
        changeColumnWidthListener(lastChangeCol);

        jobsTable.setItems(jobsForMainForm.getListOfJobs());
    }

    private void startJob(Job job)
    {
        if (job.getJobStatus() != Job.JobStatusListing.Впроцессе)
            job.start();
        else
            writeToLog("Can't start job. " + job.getJobName() + " (#" + job.getJobID() + ")" + " is in processing");
    }

    private void writeToLog(String text)     //вывод в лог сообщения
    {
        Platform.runLater(
            () ->
            {
                System.out.println("(MainController) (writeToLog) Log msg:  " + text);
                Date date = new Date();
                SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

                try {
                    logTextArea.setText(formatForDateNow.format(date) + ": " + text + "\n" + logTextArea.getText());
                } catch (Exception e) {
                    System.out.println("(MainController) Error on write to log: " + e);
                }
            }
        );
    }

    private void trayMessage (String text)
    {
        Platform.runLater(
                () -> {
                    try {
                        String caption = "Jenkins Downloader";  //заголовок сообщения
                        if (AppSettings.isShowNotifications())
                            main.getTrayIcon().displayMessage(caption, text, TrayIcon.MessageType.INFO); //метод отображения сообщения в трее
                    }
                    catch (Exception e)
                    {
                        System.out.println("(MainController) Can't display tray message: " + e);
                    }
                }
        );
    }

    private void setVisibleProgressBar(boolean status)
    {
        progressBar.setVisible(status);
    }

    private Job getSelectedJob()
    {
        TableView.TableViewSelectionModel<Job> selectionModel = jobsTable.getSelectionModel();

        try {
            return selectionModel.getSelectedItem();
        }
        catch (NullPointerException e)
        {
            System.out.println("(MainController) No job selected! Error: " + e);
            writeToLog("Choose job for download!");
            setStatus(ClientStatus.Connected);
            return null;
        }
    }

    private Runnable download (Job job, double sizeOfLastBuild, File file)
    {
        Runnable download = () -> {
            if (sizeOfLastBuild == -1.0)
                writeToLog("Start downloading: " + job.getJobName() + " (#" + job.getJobID() + ")");
            else {
                String formattedSize = new DecimalFormat("#0.00").format((sizeOfLastBuild / 1024) / 1024);
                writeToLog("Start downloading: " + job.getJobName() + " (#" + job.getJobID() + "), " + formattedSize + "Mb");
            }

            setStatus(ClientStatus.Downloading, job);

            job.download(file);

            writeToLog("Download complete: " + job.getJobName() + " (#" + job.getJobID() + ")");
            trayMessage("Download complete: " + job.getJobName() + " (#" + job.getJobID() + ")");
            setStatus(ClientStatus.Connected);
        };

        return download;
    }

    private void startJobDownload(Job job)
    {
        String path = AppSettings.getSavePath();
        File folder = new File(path + "\\" + job.getJobName() + "\\");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        double size = job.getSize();
        setVisibleProgressBar(false);

        progressBar.setProgress(0);
        File file = new File(folder, job.getJobID() + ".zip");

        if (!file.exists())
        {
            Thread downloadThread = new Thread(download(job, size, file));
            downloadThread.start();

            if(size != -1.0) {
                setVisibleProgressBar(true);
                Runnable setProgress = () -> {
                    while (downloadThread.isAlive())
                        progressBar.setProgress(file.length() / size);
                    //TODO: хранить рамер скаченного фала в джобе
                };
                Thread setProgressThread = new Thread(setProgress);
                setProgressThread.start();
            }
    }
        else {
            writeToLog(job.getJobName() + " (#" + job.getJobID() + ") already exists");
            setStatus(ClientStatus._lastStatus);
        }
    }

    private void setStatusText (String text, Color color)
    {
        try {
            Platform.runLater(
                    () -> {
                        statusLabel.setText(text);
                        statusLabel.setTextFill(color);
                    }
            );
        }
        catch (Exception e) {
            System.out.println("(MainController) Label error: " + e);
        }
    }

    public JenkinsJobs getListOfJobs()
    {
        return allFoundJobs;
    }

    @FXML
    private void clearLog()
    {
        logTextArea.clear();
    }

    @FXML
    private void showHideLog()
    {
        if (hideLog)
        {
            hideLogButton.setText("Hide log");
            logAnchorPane.setVisible(true);
            Main.getStage().setWidth(Main.SCENE_WIDTH);
        }
        else
        {
            hideLogButton.setText("Show log");
            logAnchorPane.setVisible(false);
            Main.getStage().setWidth(Main.SCENE_WIDTH - logAnchorPane.getWidth());
        }
        hideLog = !hideLog;
    }

    private void setStatus (ClientStatus status)    //установка статуса клиента (приложения)
    {

        try
        {
            switch (status)
            {
                case _lastStatus:
                    actualStatus = lastStatus;
                    break;
                case Connecting:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Connecting;
                    setStatusText("Finding Jenkins on: \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Connected:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Connected;
                    setStatusText("Connected to \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
                    progressIndicator.setVisible(false);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(false);
                    refreshButton.setDisable(false);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Updating:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Updating;
                    setStatusText("Getting job list from \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Extracting:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Extracting;
                    setStatusText("Extracting...", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Downloading:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Downloading;
                    setStatusText("Downloading in " + AppSettings.getSavePath(), Color.GREEN);
                    progressIndicator.setVisible(true);
                    //progressBar.setVisible(true);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Disconnected:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Disconnected;
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Can't find Jenkins server on \"" + AppSettings.getServerAddress() + "\"", Color.RED);
                    progressIndicator.setVisible(false);
                    downloadButton.setDisable(false);
                    refreshButton.setDisable(false);
                    settingsButton.setDisable(false); //доступ к настрйокам false - есть, true - нет
                    break;
                default:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Disconnected;
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Status error", Color.RED);
                    progressIndicator.setVisible(false);
                    refreshButton.setDisable(true);
                    downloadButton.setDisable(true);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
            }
        }
        catch (Exception e)
        {
            System.out.println("(MainController) Error on change status: " + e);
        }

    }

    private void setStatus (ClientStatus status, Job job)    //установка статуса клиента с именем джобы (приложения)
    {

        try
        {
            switch (status)
            {
                case _lastStatus:
                    actualStatus = lastStatus;
                    break;
                case Connecting:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Connecting;
                    setStatusText("Finding Jenkins on: \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);   //доступ к настрйокам false - есть, true - нет
                    break;
                case Connected:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Connected;
                    setStatusText("Connected to \"" + AppSettings.getServerAddress()+ "\"", Color.GREEN);
                    progressIndicator.setVisible(false);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(false);
                    refreshButton.setDisable(false);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Updating:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Updating;
                    setStatusText("Getting job list from \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);   //доступ к настрйокам false - есть, true - нет
                    break;
                case Extracting:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Extracting;
                    setStatusText("Extracting " + job.getJobName() + " (#" + job.getJobID() + ") in \"" + AppSettings.getSavePath() + "\\" + job.getJobName() + "\"", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);   //доступ к настрйокам false - есть, true - нет
                    break;
                case Downloading:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Downloading;
                    setStatusText("Downloading \"" + job.getJobName() + " (#" + job.getJobID() + ")\" in \"" + AppSettings.getSavePath() + "\\" + job.getJobName() + "\\" + job.getJobID() + ".zip\"", Color.GREEN);
                    progressIndicator.setVisible(true);
                    //progressBar.setVisible(true);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);   //доступ к настрйокам false - есть, true - нет
                    break;
                case Disconnected:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Disconnected;
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Can't find Jenkins server on \"" + AppSettings.getServerAddress() + "\"", Color.RED);
                    progressIndicator.setVisible(false);
                    downloadButton.setDisable(false);
                    refreshButton.setDisable(false);
                    settingsButton.setDisable(false); //доступ к настрйокам false - есть, true - нет
                    break;
                default:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Disconnected;
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Status error", Color.RED);
                    progressIndicator.setVisible(false);
                    refreshButton.setDisable(true);
                    downloadButton.setDisable(true);
                    settingsButton.setDisable(false);   //доступ к настрйокам false - есть, true - нет
                    break;
            }
        }
        catch (Exception e)
        {
            System.out.println("(MainController) Error on change status: " + e);
        }

    }

    private void changeColumnWidthListener(final TableColumn listerColumn)
    {
        listerColumn.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                String columnName = listerColumn.getText();
                //System.out.println("(MainController) (changeColumnWidthListener) Column name: " + columnName);
                switch (columnName)
                {
                    case "Job name":
                        columnName = "widthColumnJobName";
                        break;
                    case "Last success build (ID)":
                        columnName = "widthColumnJobID";
                        break;
                    case "Status of last build":
                        columnName = "widthColumnJobStatus";
                        break;
                    case "Is File?":
                        columnName = "widthColumnIsFile";
                        break;
                    case "Tag":
                        columnName = "widthColumnTagName";
                        break;
                    case "Last change":
                        columnName = "widthColumnTimeLastUpdate";
                        break;
                }

                int width = (int) (listerColumn.getWidth());
                AppSettings.changeSettingInConfig(columnName, String.valueOf(width));
            }
        });
    }

    @FXML
    private void helpButtonClick()
    {
        openHelp();
    }

    private void openSettings()  //открытие настроек
    {
        Window stage = Main.getStage();
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

            settingsStage.show();
        }
        catch (Exception e)
        {
            System.out.println("(Main) Can't open settings: " + e);
        }
    }

    private void openHelp()  //открытие help'a
    {
        Window stage = Main.getStage();
        try {
            Parent helpRoot = FXMLLoader.load(getClass().getClassLoader().getResource("sample/help.fxml"));
            Scene helpScene = new Scene(helpRoot, 800, 400);

            helpStage = new Stage();
            helpStage.setTitle("Help");
            helpStage.initModality(Modality.NONE);
            helpStage.initOwner(stage);

            helpStage.getIcons().add(new Image(helpImageURL));

            helpStage.setScene(helpScene);

            helpStage.setResizable(false);

            helpStage.show();
        }
        catch (Exception e)
        {
            System.out.println("(Main) Can't open settings: " + e);
        }
    }

    public static Stage getHelpStage() { return helpStage;}

}
