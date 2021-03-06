package ui.listbook;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jfoenix.controls.JFXSnackbar;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import database.DatabaseHandler;
import ui.addbook.BookAddController;
import ui.main.MainController;

import static util.Constant.snackbar;
import static util.LibraryAssistantUtil.setStageIcon;
import static util.alert.AlertMaker.showErrorMessage;
import static util.alert.AlertMaker.showSimpleAlert;

public class BookListController {
    private ObservableList<Book> list = FXCollections.observableArrayList();

    @FXML
    private TableView<Book> bookTable;
    @FXML
    private TableColumn<Book, String> titleCol;
    @FXML
    private TableColumn<Book, String> idCol;
    @FXML
    private TableColumn<Book, String> authorCol;
    @FXML
    private TableColumn<Book, String> publisherCol;
    @FXML
    private TableColumn<Book, Boolean> availabilityCol;

    @FXML
    public void initialize() {
        initCol();
        loadData();
    }

    private void initCol() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        publisherCol.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        availabilityCol.setCellValueFactory(new PropertyValueFactory<>("availability"));
        availabilityCol.setSortable(false);
    }

    private void loadData() {
        list.clear();
        
        DatabaseHandler handler = DatabaseHandler.getInstance();
        String qu = "SELECT * FROM BOOK";
        ResultSet rs = handler.execQuery(qu);
        try {
            while (rs != null && rs.next()) {
                String title = rs.getString("title");
                String author = rs.getString("author");
                String id = rs.getString("id");
                String publisher = rs.getString("publisher");
                Boolean avail = rs.getBoolean("isAvail");

                list.add(new Book(title, id, author, publisher, avail));
            }
        } catch (SQLException ex) {
            Logger.getLogger(BookAddController.class.getName()).log(Level.SEVERE, null, ex);
        }

        bookTable.setItems(list);
    }

    @FXML
    private void handleBookDeleteOption() {
        //Fetch the selected row
        Book selectedForDeletion = bookTable.getSelectionModel().getSelectedItem();
        if (selectedForDeletion == null) {
            showErrorMessage("No book selected", "Please select a book for deletion.");
            return;
        }
        if (DatabaseHandler.getInstance().isBookAlreadyIssued(selectedForDeletion)) {
            showErrorMessage("Cant be deleted", "This book is already issued and cant be deleted.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Deleting book");
        alert.setContentText("Are you sure want to delete the book '" + selectedForDeletion.getTitle() + "' ?");
        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            Boolean result = DatabaseHandler.getInstance().deleteBook(selectedForDeletion);
            if (result) {
                snackbar.fireEvent(new JFXSnackbar.SnackbarEvent("《" + selectedForDeletion.getTitle() + "》 was deleted successfully."));
                list.remove(selectedForDeletion);
            } else {
                showSimpleAlert("Failed", selectedForDeletion.getTitle() + " could not be deleted");
            }
        } else {
            snackbar.fireEvent(new JFXSnackbar.SnackbarEvent("Deletion process cancelled"));
        }
    }

    @FXML
    private void handleBookEditOption() {
        //Fetch the selected row
        Book selectedForEdit = bookTable.getSelectionModel().getSelectedItem();
        if (selectedForEdit == null) {
            showErrorMessage("No book selected", "Please select a book for edit.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addbook/add_book.fxml"));
            Parent parent = loader.load();

            BookAddController controller = loader.getController();
            controller.inflateUI(selectedForEdit);
            
            Stage stage = new Stage(StageStyle.DECORATED);
            setStageIcon(stage);
            stage.setTitle("Edit Book");
            stage.setScene(new Scene(parent));
            stage.show();
            
            stage.setOnCloseRequest((e) -> handleRefresh());
        } catch (IOException ex) {
            Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleRefresh() {
        System.out.println("handleRefresh");
        loadData();
    }

    public static class Book {
        private final SimpleStringProperty title;
        private final SimpleStringProperty id;
        private final SimpleStringProperty author;
        private final SimpleStringProperty publisher;
        private final SimpleBooleanProperty availability;

        public Book(String title, String id, String author, String pub, Boolean avail) {
            this.title = new SimpleStringProperty(title);
            this.id = new SimpleStringProperty(id);
            this.author = new SimpleStringProperty(author);
            this.publisher = new SimpleStringProperty(pub);
            this.availability = new SimpleBooleanProperty(avail);
        }

        public String getTitle() {
            return title.get();
        }

        public String getId() {
            return id.get();
        }

        public String getAuthor() {
            return author.get();
        }

        public String getPublisher() {
            return publisher.get();
        }

        public Boolean getAvailability() {
            return availability.get();
        }
    }
}
