package sample;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class HelpController {

    @FXML
    private TextArea helpTextArea;

    @FXML
    private  void initialize()
    {
        helpTextArea.setEditable(false);
        helpTextArea.appendText("Jenkins downloader\n");
        helpTextArea.appendText("Основные возможности:\n");
        helpTextArea.appendText("- получение списка работ с сервера Jenkins по заданному в настройках адресу;\n");
        helpTextArea.appendText("- отображение полученного списка работ в таблице с возможностью скрытия столбцов с информацией;\n");
        helpTextArea.appendText("- автоматическое отслеживание изменений статусов работ;\n");
        helpTextArea.appendText("- отображение информационных сообщений в трее (изменения состояния работ, начало/окончание скачивания);\n");
        helpTextArea.appendText("- логирование изменений статусов работ;\n");
        helpTextArea.appendText("- выбор папки для скачивания работ;\n");
        helpTextArea.appendText("- скачивание последнего успешного билда для выбранной работы;\n");
        helpTextArea.appendText("- скрытие/отображение в таблице работ, которые не содержат файлов для скачивания;\n");
        helpTextArea.appendText("- задание пользовательских тэгов для работ. Тэги отображаются в сообщениях вместо инженерных имен работ, полученных с сервера;\n");
        helpTextArea.appendText("- автоматическое сохранение всех пользовательских настроек в конфиг-файл;\n");
        helpTextArea.appendText("- автоматическая загрузка всех пользовательских настроек из конфиг-файла при запуске приложения;\n");
    }

    @FXML
    private  void closeButtonClick()
    {
        Stage mainStage = MainController.getHelpStage();
        mainStage.close();
    }


}
