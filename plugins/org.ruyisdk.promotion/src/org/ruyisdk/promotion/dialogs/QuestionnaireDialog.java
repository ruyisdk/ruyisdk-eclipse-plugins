package org.ruyisdk.promotion.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.ruyisdk.promotion.Activator;

/**
 * Startup dialog showing the questionnaire QR code and direct link.
 */
public class QuestionnaireDialog extends Dialog {

    public static final String QUESTIONNAIRE_URL =
            "https://docs.qq.com/form/page/DVUxIZURaY2hGRnFL";
    private static final String QRCODE_PATH = "images/questionnaire-qrcode.png";

    private Image qrCodeImage;

    /**
     * Creates the startup questionnaire dialog.
     *
     * @param parentShell parent shell
     */
    public QuestionnaireDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("RuyiSDK Questionnaire");
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final var area = (Composite) super.createDialogArea(parent);
        final var container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        final var title = new Label(container, SWT.WRAP);
        title.setText("Scan the QR code or click the link to open our questionnaire:");
        title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final var imageLabel = new Label(container, SWT.NONE);
        imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        qrCodeImage = loadQrCodeImage();
        if (qrCodeImage != null) {
            imageLabel.setImage(qrCodeImage);
        }

        final var link = new Link(container, SWT.WRAP);
        link.setText(String.format("<a href=\"%s\">%s</a>", QUESTIONNAIRE_URL, QUESTIONNAIRE_URL));
        link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        link.addListener(SWT.Selection, event -> Program.launch(event.text));

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Control createContents(Composite parent) {
        final var control = super.createContents(parent);
        getShell().setMinimumSize(420, 520);
        return control;
    }

    @Override
    public boolean close() {
        if (qrCodeImage != null && !qrCodeImage.isDisposed()) {
            qrCodeImage.dispose();
            qrCodeImage = null;
        }
        return super.close();
    }

    private Image loadQrCodeImage() {
        final ImageDescriptor descriptor =
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, QRCODE_PATH);
        if (descriptor == null) {
            Activator.getLogger()
                    .logWarning("Questionnaire QR code image was not found: " + QRCODE_PATH);
            return null;
        }
        return descriptor.createImage();
    }
}
