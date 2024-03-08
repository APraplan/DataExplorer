package gde.ui.dialog;

import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
/**
* This class enables a simple capability to select a number, for instance a font size
*/
public class FontSizeDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger						log	= Logger.getLogger(FontSizeDialog.class.getName());

	 Shell dialogShell;
	 Combo fontSizeCombo;
	 Button okButton;
	int selectedFontSize = 50;


	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			FontSizeDialog inst = new FontSizeDialog(shell, SWT.NULL);
			inst.open(new String[]{"20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70"}, 5);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FontSizeDialog(Shell parent, int style) {
		super(parent, style);
	}

	public int open(String[] sizeValues, int selectionIndex) {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0727));
			RowLayout dialogShellLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
			dialogShell.setLayout(dialogShellLayout);
			{
				fontSizeCombo = new Combo(dialogShell, SWT.BORDER);
				RowData fontSizeComboLData = new RowData();
				fontSizeComboLData.width = 50;
				//fontSizeComboLData.height = 20;
				fontSizeCombo.setLayoutData(fontSizeComboLData);
				fontSizeCombo.setItems(sizeValues);
				fontSizeCombo.select(selectionIndex);
				fontSizeCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "fontSizeCombo.widgetSelected, event="+evt);
						selectedFontSize = Integer.parseInt(fontSizeCombo.getText().trim());
					}
				});
			}
			{
				okButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				RowData okButtonLData = new RowData();
				okButtonLData.width = 50;
				//okButtonLData.height = 23;
				okButton.setLayoutData(okButtonLData);
				okButton.setText("OK");
				okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "okButton.widgetSelected, event="+evt);
						dialogShell.dispose();
					}
				});
			}
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "dialogShell.widgetDisposed, event="+evt);
					selectedFontSize = Integer.parseInt(fontSizeCombo.getText().trim());
				}
			});
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return selectedFontSize;
	}
	
}
