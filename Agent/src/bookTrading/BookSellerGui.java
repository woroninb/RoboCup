package bookTrading;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


class BookSellerGui extends JFrame {	
	private BookSellerAgent myAgent;
	
	private JTextField titleField, priceField;
	
	BookSellerGui(BookSellerAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Tytul:"));
		titleField = new JTextField(15);
		p.add(titleField);
		p.add(new JLabel("Cena:"));
		priceField = new JTextField(15);
		p.add(priceField);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Dodaj");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String title = titleField.getText().trim();
					String price = priceField.getText().trim();
					myAgent.updateCatalogue(title, Integer.parseInt(price));
					titleField.setText("");
					priceField.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(BookSellerGui.this, "Nieprawidlowe wartosci. " + e.getMessage(), "B�ad", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void display() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		setVisible(true);
	}	
}
