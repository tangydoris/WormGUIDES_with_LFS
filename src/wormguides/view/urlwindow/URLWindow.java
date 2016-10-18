/*
 * Bao Lab 2016
 */

package wormguides.view.urlwindow;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import wormguides.loaders.ImageLoader;
import wormguides.models.Rule;
import wormguides.util.AppFont;

import static java.awt.Toolkit.getDefaultToolkit;
import static java.util.Objects.requireNonNull;

import static javafx.geometry.Pos.CENTER;
import static javafx.scene.layout.HBox.setHgrow;

import static wormguides.util.URLGenerator.generateAndroid;

public class URLWindow extends AnchorPane {

    private final TextField urlField;
    private final Button resetBtn;
    private final Button closeBtn;
    private final Clipboard cb;

    private final ObservableList<Rule> rulesList;
    private final IntegerProperty timeProperty;
    private final DoubleProperty rotateXAngleProperty;
    private final DoubleProperty rotateYAngleProperty;
    private final DoubleProperty rotateZAngleProperty;
    private final DoubleProperty translateXProperty;
    private final DoubleProperty translateYProperty;
    private final DoubleProperty zoomProperty;
    private final DoubleProperty othersOpacityProperty;

    private String urlString;

    public URLWindow(
            final ObservableList<Rule> rulesList,
            final IntegerProperty timeProperty,
            final DoubleProperty rotateXAngleProperty,
            final DoubleProperty rotateYAngleProperty,
            final DoubleProperty rotateZAngleProperty,
            final DoubleProperty translateXProperty,
            final DoubleProperty translateYProperty,
            final DoubleProperty zoomProperty,
            final DoubleProperty othersOpacityProperty) {

        super();
        setPrefWidth(430);

        this.rulesList = requireNonNull(rulesList);
        this.timeProperty = requireNonNull(timeProperty);
        this.rotateXAngleProperty = requireNonNull(rotateXAngleProperty);
        this.rotateYAngleProperty = requireNonNull(rotateYAngleProperty);
        this.rotateZAngleProperty = requireNonNull(rotateZAngleProperty);
        this.translateXProperty = requireNonNull(translateXProperty);
        this.translateYProperty = requireNonNull(translateYProperty);
        this.zoomProperty = requireNonNull(zoomProperty);
        this.othersOpacityProperty = requireNonNull(othersOpacityProperty);

        cb = getDefaultToolkit().getSystemClipboard();
        final Tooltip tooltip = new Tooltip("copy");

        final VBox vBox = new VBox();
        vBox.setSpacing(10);
        AnchorPane.setTopAnchor(vBox, 10.0);
        AnchorPane.setLeftAnchor(vBox, 10.0);
        AnchorPane.setRightAnchor(vBox, 10.0);
        AnchorPane.setBottomAnchor(vBox, 10.0);
        getChildren().add(vBox);

        final HBox androidHBox = new HBox(10);
        urlField = new TextField();
        urlField.setFont(AppFont.getFont());
        urlField.setPrefHeight(28);
        urlField.setEditable(false);
        urlField.setStyle("-fx-focus-color: -fx-outer-border; -fx-faint-focus-color: transparent;");
        setHgrow(urlField, Priority.ALWAYS);
        Button androidCopyBtn = new Button();
        androidCopyBtn.setPrefSize(28, 28);
        androidCopyBtn.setMinSize(28, 28);
        androidCopyBtn.setMaxSize(28, 28);
        androidCopyBtn.setTooltip(tooltip);
        androidCopyBtn.setStyle("-fx-focus-color: -fx-outer-border; -fx-faint-focus-color: transparent;");
        androidCopyBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        androidCopyBtn.setGraphic(ImageLoader.getCopyIcon());
        androidCopyBtn.setOnAction(arg0 -> {
            StringSelection ss = new StringSelection(urlField.getText());
            cb.setContents(ss, null);
        });
        androidHBox.getChildren().addAll(urlField, androidCopyBtn);

        resetBtn = new Button("Generate");
        resetBtn.setPrefWidth(100);
        resetBtn.setStyle("-fx-focus-color: -fx-outer-border; -fx-faint-focus-color: transparent;");
        resetBtn.setFont(AppFont.getFont());
        resetBtn.setOnAction(arg0 -> resetURLs());

        closeBtn = new Button("Close");
        closeBtn.setPrefWidth(100);
        closeBtn.setStyle("-fx-focus-color: -fx-outer-border; -fx-faint-focus-color: transparent;");
        closeBtn.setFont(AppFont.getFont());

        final HBox hBox = new HBox();
        hBox.setSpacing(20);
        hBox.setAlignment(CENTER);
        hBox.getChildren().addAll(resetBtn, closeBtn);

        vBox.getChildren().addAll(androidHBox, hBox);
    }

    public void resetURLs() {
        urlString = generateAndroid(
                rulesList,
                timeProperty.get(),
                rotateXAngleProperty.get(),
                rotateYAngleProperty.get(),
                rotateZAngleProperty.get(),
                translateXProperty.get(),
                translateYProperty.get(),
                zoomProperty.get(),
                othersOpacityProperty.get());
        urlField.setText(urlString);
    }

    public Button getCloseButton() {
        return closeBtn;
    }
}
