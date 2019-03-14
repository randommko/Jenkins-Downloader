package sample;


import java.awt.event.TextEvent;
import java.text.DecimalFormat;
import java.util.Iterator;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
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
//    @FXML
//    private volatile Label settingsLabel;
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


    private Main main;
    private static JenkinsJobs jobsForMainForm;
    private static JenkinsJobs allFoundJobs;
    public enum ClientStatus {Disconnected, Connected, Downloading, Extracting, Connecting, Updating, _lastStatus}
    private ClientStatus lastStatus, actualStatus;
    private static final String settingsImageURL = "image/settings(small).png";
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
         Runnable updateStatus  = () -> {
            String out;
            String serverAddress = AppSettings.getServerAddress();

            do {
                if (AppSettings.isAutoUpdate())
                {
                    try {

                        out = allFoundJobs.refreshStatusOfAllJobs(serverAddress);   //обновляем статусы всех работ. Возврашает строку с навзаниями работ у которых статус изменился

                        if (!out.equals("No jobs has been updated")) {
                            writeToLog(out);
                            trayMessage(out);
                        }
                        TimeUnit.SECONDS.sleep(1);
                    }
                    catch (Exception e) {
                        System.out.println("(MainController) Error on job status updating: " + e);
                        if (actualStatus == ClientStatus.Connected)
                            setStatus(ClientStatus.Disconnected);
                        try
                        {
                            TimeUnit.SECONDS.sleep(10);
                        }
                        catch (Exception err)
                        {
                            System.out.println("(MainController) Can't call sleep method: " + err);
                        }
                    }
                }
                else
                    try
                    {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    catch (Exception err)
                    {
                        System.out.println("(MainController) Can't call sleep method: " + err);
                    }
            } while (true);
        };
        return updateStatus;
    }

    @FXML
    private void connectToServer()              //нажатие кнопки "Refresh"
    {
        connect();
    }

    private void connect()
    {
        try
        {
            jobsForMainForm.clear();
            allFoundJobs.clear();

            String serverAddress = AppSettings.getServerAddress();
            //serverAddress = serverAddressTextField.getText();
            Document document = Jsoup.connect(serverAddress).get(); //получаем копию страницы в виде документа

            Elements elements = document.select("title");   //создаем список tr-элементов страницы которые содержат текст "job-status" внутри себя
            Iterator iterator = elements.iterator();  //создаем итератор по элментам страницы содержащим имена работ
            String serverName = "";
            while ( iterator.hasNext() )
            {
                Element element = (Element) iterator.next();
                serverName = element.attr("Jenkins");
            }
            if ( !serverName.equals("") )
            {
                writeToLog("Server not found");
                setStatus(ClientStatus.Disconnected);
                trayMessage("Server not found");
            }
            else {
                setStatus(ClientStatus.Updating);
                trayMessage("Updating job list");

                refreshingAllJobsStatus();
            }
        }
        catch (IOException e)
        {
            writeToLog("Enter server address: \"http://[address]:[port]\"");
            setStatus(ClientStatus.Disconnected);
            trayMessage("Server not found");
        }
        catch (IllegalArgumentException err)
        {
            writeToLog("Enter correct server address");
            setStatus(ClientStatus.Disconnected);
        }
    }

    private void refreshingAllJobsStatus()
    {
        String serverAddress = AppSettings.getServerAddress();
        Runnable runnableGetJobList = () -> {
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
            catch (IOException e) {
                writeToLog("Server not found!");
                setStatus(ClientStatus.Disconnected);

            }
            catch (IllegalArgumentException err)
            {
                writeToLog("Port out of range");
                setStatus(ClientStatus.Disconnected);
            }
            catch (Exception error)
            {
                System.out.println("(MainController) (refreshingAllJobsStatus) Unknown error: " + error);
                setStatus(ClientStatus.Disconnected);
            }
        };

        Thread threadGetJobList = new Thread(runnableGetJobList);
        threadGetJobList.start();
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
        main.openSettings();

        //TODO: проверить, можно ли убрать этот костыль
        Runnable runnable = () -> {
                Window settings = main.getSettingsStage();
                while (settings.isShowing())
                {
                    try
                    {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    catch (InterruptedException e)
                    {

                    }
                }
                loadTableConfig();
            };
        Thread thread = new Thread(runnable);   //необходимо для загузрки актуального конфига после закрытия окна настроек
        thread.start();

    }

    private void initWindow()
    {
        main = new Main();
        //logTextArea = new TextArea();
        hideLog = false;


        try {
            Image settings = new Image(settingsImageURL);
            settingsButton.setGraphic(new ImageView(settings));
            //settingsLabel.setText("Settings");
        }
        catch (Exception e)
        {
            System.out.println("(MainController) Can't set image for settings button: " + e);
        }

        logTextArea.setEditable(false);

        configTable();
    }

    public void loadTableConfig()
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
                System.out.println("(MainController) (writeToLog) msg:  " + text);
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

    public void trayMessage (String text)
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

    public void setVisibleProgressBar(boolean status)
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

    private double getLastSize(File folder)
    {
        double size = -1;
        File[] folderEntries = folder.listFiles();
        for (File entry : folderEntries) {
            if (entry.isDirectory()) {
                continue;
            }
            size = entry.length();
        }

        return size;
    }

    private Runnable download (Job job, double sizeOfLastBuild, File file)
    {
        Runnable download = () -> {
            if (sizeOfLastBuild == -1)
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

        double sizeOfLastBuild = getLastSize(folder);
        if(sizeOfLastBuild == 0)
            setVisibleProgressBar(false);

        progressBar.setProgress(0);
        File file = new File(folder, job.getJobID() + ".zip");

        if (!file.exists())
        {
            Thread downloadThread = new Thread(download(job, sizeOfLastBuild, file));
            downloadThread.start();

            if(sizeOfLastBuild != -1) {
                Runnable setProgress = () -> {
                    while (downloadThread.isAlive())
                        progressBar.setProgress(file.length() / sizeOfLastBuild);
                };
                Thread setProgressThread = new Thread(setProgress);
                setProgressThread.start();
            }
            else
                setVisibleProgressBar(false);
        }
        else {
            writeToLog(job.getJobName() + " (#" + job.getJobID() + ") already exists");
            setStatus(ClientStatus._lastStatus);
        }
    }

    public void setStatusText (String text, Color color)
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
            main.getStage().setWidth(Main.SCENE_WIDTH);

            //root.setPrefWidth(1000);
        }
        else
        {
            hideLogButton.setText("Show log");
            logAnchorPane.setVisible(false);
            main.getStage().setWidth(Main.SCENE_WIDTH - 500 + 15);
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
                    setStatusText("Connecting to \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
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
                    progressBar.setVisible(true);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);    //доступ к настрйокам false - есть, true - нет
                    break;
                case Disconnected:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Disconnected;
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Can't find jenkins server on \"" + AppSettings.getServerAddress() + "\"", Color.RED);
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

    private void setStatus (ClientStatus status, Job job)    //установка статуса клиента (приложения)
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
                    setStatusText("Connecting to \"" + AppSettings.getServerAddress() + "\"", Color.GREEN);
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
                    progressBar.setVisible(true);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    settingsButton.setDisable(false);   //доступ к настрйокам false - есть, true - нет
                    break;
                case Disconnected:
                    lastStatus = actualStatus;
                    actualStatus = ClientStatus.Disconnected;
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Can't find jenkins server on \"" + AppSettings.getServerAddress() + "\"", Color.RED);
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
        main.openHelp();
    }

}
