import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FactorialApplet extends Applet implements ActionListener {
    TextField inputField, resultField;
    Button computeButton;

    public void init() {
        Label inputLabel = new Label("Enter a number:");
        inputField = new TextField(10);

        computeButton = new Button("Compute");
        computeButton.addActionListener(this);

        Label resultLabel = new Label("Factorial:");
        resultField = new TextField(10);
        resultField.setEditable(false);

        add(inputLabel);
        add(inputField);
        add(computeButton);
        add(resultLabel);
        add(resultField);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            int number = Integer.parseInt(inputField.getText());
            long factorial = 1;
            for (int i = 1; i <= number; i++) {
                factorial *= i;
            }
            resultField.setText(String.valueOf(factorial));
        } catch (NumberFormatException ex) {
            resultField.setText("Invalid Input");
        }
    }
}
