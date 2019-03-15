package sample;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.util.ArrayList;

public class TagSettingsController
{
    @FXML
    TableColumn<Job, String> jobNameColumn;
    @FXML
    TableColumn<Job, String> jobTagColumn;
    @FXML
    TableView<Job> jobsTable;
    @FXML
    Button saveButton, cancelButton;

    private JenkinsJobs jobs;
    private ArrayList<String> list;

    @FXML
    private void initialize() //метод в котором выполняется код при запуске приложения
    {
        cancelButton.setVisible(false);     //Отображать кнопку "Отмена"

        list = new ArrayList<>();

//        main = new Main();
        MainController mainController = new MainController();

        this.jobs = mainController.getListOfJobs();

        jobsTable.setEditable(true);
        jobNameColumn.setEditable(false);
        jobTagColumn.setEditable(true);

        jobNameColumn.setCellValueFactory(new PropertyValueFactory<>("jobName"));
        jobTagColumn.setCellValueFactory(new PropertyValueFactory<>("visibleName"));

        jobsTable.setItems(this.jobs.getListOfJobs());

        jobTagColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        jobTagColumn.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent<Job, String>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<Job, String> cell) {
                        Job job = cell.getTableView().getItems().get(cell.getTablePosition().getRow());
                        job.setVisibleName(cell.getNewValue());

                        System.out.println("(TagSettingsController) (setOnEditCommit) Changed: " + cell.getNewValue());

                        list.add(job.getJobName() + ":" + cell.getNewValue());
                    }
                }
        );
    }

    public void saveButton()
    {
        for (String string: list)
        {
            String jobName = string.substring(0, string.indexOf(":"));  //имя джобы = весь тест до двоеточия
            String newTag = string.substring(string.indexOf(":") + 1, string.length()); //тэг = весь тест после двоеточия
            AppSettings.changeSettingInConfig(jobName + "_tag", newTag); //записываем тэг в конфиг
        }

        Stage mainStage = SettingsController.getTagSettingsStage();
        mainStage.close();
    }

    public void cancelButton()
    {
        Stage mainStage = SettingsController.getTagSettingsStage();
        mainStage.close();
    }
}
