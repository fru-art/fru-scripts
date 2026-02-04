package com.osmbtoolkit.options;

import com.osmbtoolkit.script.ToolkitScript;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.prefs.Preferences;

public abstract class Options<T extends ToolkitScript> {
  public static class Dropdown {
    public final ComboBox<String> comboBox;
    public final Node node;

    public Dropdown(Node node, ComboBox<String> comboBox) {
      this.comboBox = comboBox;
      this.node = node;
    }
  }

  protected final T script;

  protected final Preferences preferences;
  protected VBox root;
  protected Scene scene;

  private boolean confirmed = false;

  public Options(T script) {
    this.script = script;
    this.preferences = Preferences.userNodeForPackage(this.getClass());
  }

  public void show() {
    if (this.script.stopped()) return;
    // Do not create root and scene in the constructor because they call getChildren, which won't be available until
    // after initialization because it's abstract.
    if (this.root == null) this.root = createRoot();
    if (this.scene == null) this.scene = createScene(this.root);
    this.script.getStageController().show(this.scene, this.getTitle(), this.getResizable());
  }

  public abstract List<Node> getChildren();

  // INPUTS
  protected Dropdown getDropdown(String label, List<String> options, String defaultValue) {
    ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableList(options));
    comboBox.setValue(this.preferences.get(label, defaultValue));
    comboBox.setOnAction(event -> this.preferences.put(label, comboBox.getValue()));
    HBox hBox = new HBox(12.0, new Label(label), comboBox);
    hBox.setAlignment(Pos.CENTER_LEFT);
    return new Dropdown(hBox, comboBox);
  }

  protected CheckBox getCheckBox(String label, boolean defaultValue) {
    CheckBox checkBox = new CheckBox(label);
    checkBox.setSelected(this.preferences.getBoolean(label, defaultValue));
    checkBox.setOnAction(event -> this.preferences.putBoolean(label, checkBox.isSelected()));
    return checkBox;
  }

  protected Region getHSeparator() {
    Region separator = new Region();
    separator.setPrefHeight(1);
    separator.getStyleClass().add("separator");
    return separator;
  }

  protected RadioButton getRadioButton(String label, boolean defaultValue, ToggleGroup toggleGroup) {
    RadioButton radioButton = new RadioButton(label);
    radioButton.setSelected(this.preferences.getBoolean(label, defaultValue));
    radioButton.setToggleGroup(toggleGroup);
    radioButton.setOnAction(event -> {
      for (Toggle toggle : toggleGroup.getToggles()) {
        if (!(toggle instanceof RadioButton)) continue;
        this.preferences.putBoolean(((RadioButton) toggle).getText(), false);
      }
      this.preferences.putBoolean(label, true);
    });
    return radioButton;
  }

  protected Region getVSeparator() {
    Region separator = new Region();
    separator.setPrefWidth(1);
    separator.getStyleClass().add("separator");
    return separator;
  }

  // CUSTOMIZABLES
  protected String getConfirmText() {
    return "Confirm";
  }

  protected boolean getResizable() {
    return true;
  }

  protected String getTitle() {
    return script.scriptDefinition.name() + " v" + script.scriptDefinition.version();
  }

  protected int getWidth() {
    return 360;
  }

  protected void onConfirm(ActionEvent actionEvent) {
  }

  private HBox createConfirmButton() {
    String author = script.scriptDefinition.author();
    boolean hasAuthor = author != null && !author.isEmpty();
    Node promotion;
    String promotionSecondaryStyles = "-fx-fill: #9e9e9e;-fx-font-style: italic;";

    if (hasAuthor) {
      VBox promotionVBox = new VBox();
      Text madeByText = new Text("Made by");
      madeByText.setStyle(promotionSecondaryStyles);

      HBox authorHBox = new HBox();
      Text authorText = new Text(author);
      authorText.setStyle("-fx-font-size: 14px;");

      String logo = script.getAuthorLogo();
      if (logo != null) {
        try {
          Image image = new Image(logo);

          if (!image.isError()) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            int size = 14;
            imageView.setFitHeight(size);
            imageView.setFitWidth(size);

            Circle clip = new Circle(
              imageView.getFitWidth() / 2, // Center X
              imageView.getFitHeight() / 2, // Center Y
              imageView.getFitWidth() / 2  // Radius
            );
            imageView.setClip(clip);
            clip.centerXProperty().bind(imageView.fitWidthProperty().divide(2));
            clip.centerYProperty().bind(imageView.fitHeightProperty().divide(2));
            clip.radiusProperty().bind(imageView.fitWidthProperty().divide(2));

            StackPane imagePane = new StackPane(imageView);
            imagePane.setStyle("-fx-padding: 2 4 0 2;");
            authorHBox.getChildren().add(imagePane);
          } else {
            authorHBox.getChildren().add(new Text(image.getException().getMessage()));
          }
        } catch (Exception e) {
          authorHBox.getChildren().add(new Text(e.toString()));
        }
      }

      authorHBox.getChildren().add(authorText);
      authorHBox.setAlignment(Pos.CENTER_LEFT);

      Text promotionText = new Text("with OSMB Toolkit");
      promotionText.setStyle(promotionSecondaryStyles);

      promotionVBox.getChildren().addAll(madeByText, authorHBox, promotionText);
      promotion = promotionVBox;
    } else {
      Text promotionText = new Text("Made with OSMB Toolkit");
      promotionText.setStyle(promotionSecondaryStyles);
      promotion = promotionText;
    }

    Region hBoxSpacer = new Region();
    HBox.setHgrow(hBoxSpacer, Priority.ALWAYS);

    Button confirmButton = new Button(getConfirmText());
    confirmButton.setOnAction(actionEvent -> {
      this.confirmed = true;
      if (this.scene != null) ((Stage) this.scene.getWindow()).close();
    });

    HBox confirmButtonContainer = new HBox(
      new VBox(promotion),
      hBoxSpacer,
      confirmButton
    );
    confirmButtonContainer.setAlignment(hasAuthor ? Pos.BOTTOM_RIGHT : Pos.CENTER_RIGHT);
    confirmButtonContainer.setStyle(hasAuthor ? "-fx-padding: 18 0 0 0;" : "-fx-padding: 6 0 0 0;");

    return confirmButtonContainer;
  }

  private VBox createRoot() {
    VBox root = new VBox();
    root.getChildren().addAll(this.getChildren());
    root.getChildren().add(createConfirmButton());
    root.getStyleClass().add("options-root");
    root.setPrefWidth(this.getWidth());
    return root;
  }

  private Scene createScene(VBox root) {
    Scene scene = new Scene(root);

    // 1. Keep the platform styles
    scene.getStylesheets().add("style.css");

    // 2. Manually load your local styles
    try (java.io.InputStream is = getClass().getResourceAsStream("/css/style.css")) {
      if (is != null) {
        // Read all bytes and convert to a UTF-8 string
        byte[] bytes = is.readAllBytes();
        String cssContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        // Encode the string as a Data URL so JavaFX doesn't have to "find" a file
        String dataUrl = "data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(cssContent.getBytes());

        scene.getStylesheets().add(dataUrl);
      } else {
        script.log(getClass(), "Error: Could not find /css/style.css via getResourceAsStream");
      }
    } catch (Exception e) {
      script.log(getClass(), "Error loading CSS: " + e.getMessage());
    }

    scene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
      newWindow.setOnCloseRequest(event -> {
        if (!this.confirmed) this.script.stop();
      });
    });
    return scene;
  }
}
