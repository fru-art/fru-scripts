package com.osmbtoolkit.options.impl;

import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.script.ToolkitScript;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;

public class UpdateAvailableOptions extends Options {
  public UpdateAvailableOptions(ToolkitScript script) {
    super(script);
  }

  @Override
  public List<Node> getChildren() {
    Text heading = new Text("Update available");
    heading.setFont(Font.font(null, FontWeight.BOLD, 18));

    Text paragraphOne = new Text("Automatic updates are currently disallowed by OSMB. Please manually install the " +
      "latest version.");
    paragraphOne.setWrappingWidth(300);

    Text paragraphTwo = new Text("Be aware that the script may not function properly without an update.");
    paragraphTwo.setWrappingWidth(300);

    VBox container = new VBox(heading, paragraphOne, paragraphTwo);
    container.setSpacing(8);

    return List.of(container);
  }

  @Override
  protected String getConfirmText() {
    return "Proceed anyway";
  }
}
