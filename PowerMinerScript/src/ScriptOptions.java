import com.osmb.api.script.Script;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.prefs.Preferences;

public abstract class ScriptOptions {
  protected static final Preferences preferences = Preferences.userNodeForPackage(ScriptOptions.class);

  protected VBox root;
  protected Scene scene;
  final Script script;
  private boolean confirmed = false;

  public ScriptOptions(Script script) {
    this.script = script;
  }

  public void show() {
    // Root and scene instantiation must be done here and not in the constructor, because the eventual implementations
    // of the abstract methods are not yet defined when the 'ScriptOptions' constructor is being run.
    if (root == null) {
      root = createRoot();
    }
    if (scene == null) {
      scene = new Scene(root);
      scene.getStylesheets().add("style.css");
      scene.windowProperty().addListener((observable, prevWindow, window) -> {
        // If user closed window and did not confirm, exit script. This callback must be set from the window property
        // listener because, once the window is created, the main thread is blocked and the window can't be accessed.
        window.setOnCloseRequest(event -> {
          if (confirmed) {
            confirmed = false;
          } else {
            script.stop();
          }
        });
      });
    }

    script.getStageController().show(scene, getTitle(), getResizable());
  }

  private VBox createRoot() {
    VBox root = new VBox();
    root.getChildren().addAll(getRootChildren());

    Button confirmButton = new Button("Confirm");
    confirmButton.setOnAction(actionEvent -> {
      if (!canConfirm(actionEvent, scene)) return;

      confirmed = true;
      onConfirm(actionEvent, scene);
      ((Stage) scene.getWindow()).close();
    });
    root.getChildren().add(confirmButton);
    root.setStyle("-fx-background-color: #636E72; -fx-padding: 18; -fx-spacing: 12; -fx-alignment: center-left");
    root.setPrefWidth(360);

    return root;
  }

  protected boolean canConfirm(ActionEvent actionEvent, Scene scene) {
    return true;
  }

  protected boolean getResizable() {
    return true;
  }

  protected String getTitle() {
    return getClass()
      .getSimpleName()
      .replaceAll("([a-z])([A-Z])", "$1 $2")
      .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
  }

  public abstract void onConfirm(ActionEvent actionEvent, Scene scene);

  public abstract List<Node> getRootChildren();
}
