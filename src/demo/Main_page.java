package demo;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

public class Main_page	{
	protected JFrame frame;
	protected JFrame Registration_frame;
	public JPanel Login_panel;
	public JPanel welcome_panel;
	public JPanel Registration_panel;
	public Connection connection = null;
	public VideoCapture capture;
	public Canvas canvas;
	public Boolean scanning=true;

	static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

	public Main_page() {		
		frame = new JFrame();
		Registration_frame = new JFrame();
		
		frame.setSize(800, 500);
		Registration_frame.setSize(800, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Login_panel = new JPanel();
		Registration_panel = new JPanel();
		welcome_panel = new JPanel();
		
		Build_Login_Page(Login_panel);
		Build_Registration_Page(Registration_panel);
		
		frame.add(Login_panel);
		Registration_frame.add(Registration_panel);
		
		Registration_frame.setVisible(false);
		frame.setVisible(true);
		
		canvas = new Canvas();
		canvas.setPreferredSize(new Dimension(400, 400));
		canvas.setBounds(10, 20, 400, 400);
		canvas.setBackground(Color.BLACK);
		Login_panel.add(canvas);
		
		String jdbcUrl = "jdbc:sqlite:/home/gokul/eclipse-workspace/demo/users_data.db";
		try {
			connection = DriverManager.getConnection(jdbcUrl);
		} catch (SQLException e3) {
			System.out.println("Unable to connect to database");
		}
		
		scan_qr_code();
		
	}

	public static void main(String[] args) {
		new Main_page();
	}

	
	protected void Build_Login_Page(JPanel panel) {
		//User Login panel
		panel.setLayout(null);
		JButton Create_acc_button = new JButton("Create new account");
		Create_acc_button.setBounds(450, 200, 190, 25);
		Create_acc_button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Registration_frame.setVisible(true);
			}
		});
		Login_panel.add(Create_acc_button);
	}
	
	//streams webcam to video and scans for qr code.
	public void scan_qr_code() {
		capture = new VideoCapture(0);
		Mat cap_image = new Mat();
		Image img = null;
		if (capture.isOpened()) {
			while(scanning) {
				capture.read(cap_image);
				MatOfByte buffer = new MatOfByte();
				Imgcodecs.imencode(".png", cap_image, buffer);
				byte ba[] = buffer.toArray();
				Result scanned_stuff=null;
				try {
					img = ImageIO.read(new ByteArrayInputStream(ba));
					LuminanceSource source = new BufferedImageLuminanceSource((BufferedImage) img);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
					Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
				      hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
					try {
						scanned_stuff = new MultiFormatReader().decode(bitmap, hints);
					} catch (NotFoundException e) {
					}
					if (scanned_stuff!=null) {
						String str = scanned_stuff.toString();
						
						Properties props = new Properties();
						props.load(new StringReader(str.substring(1, str.length() - 1).replace(", ", "\n")));       
						Map<String, String> scanned_details = new HashMap<String, String>();
						for (Map.Entry<Object, Object> e : props.entrySet()) {
							scanned_details.put((String)e.getKey(), (String)e.getValue());
						}
						if(scanned_details.get("Organization") == null) {
							System.out.println("Invalid QR code");
						}
						else if(scanned_details.get("Organization").equals("Salad_Corp")) {
							fetch_user_fromdb(scanned_details.get("username"), scanned_details.get("password"));
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					capture.release();
					break;
				}
				canvas.getGraphics().drawImage(img, 0, 0, null);
			}
			if (!scanning) {
				capture.release();
			}
		}
	}
	
	
	protected void Build_Registration_Page(JPanel panel) {
		// User Registration panel
		// username field
		panel.setLayout(null);
		JLabel username_label = new JLabel("Name");
		username_label.setBounds(10, 20, 80, 25);
		Registration_panel.add(username_label);
		JTextField usernameText = new JTextField(20);
		usernameText.setBounds(100, 20, 165, 25);
		Registration_panel.add(usernameText);
			
		// password field
		JLabel pass_label = new JLabel("Type Some Word");
		pass_label.setBounds(10, 50, 80, 25);
		Registration_panel.add(pass_label);
		JTextField pass_Text = new JTextField(20);
		pass_Text.setBounds(100, 50, 165, 25);
		Registration_panel.add(pass_Text);
		
		// email field
		JLabel email_label = new JLabel("email");
		email_label.setBounds(10, 80, 80, 25);
		Registration_panel.add(email_label);
		JTextField email_Text = new JTextField(20);
		email_Text.setBounds(100, 80, 165, 25);
		Registration_panel.add(email_Text);
		
		// Register Button
		JButton reg_button = new JButton("Register");
		reg_button.setBounds(10, 120, 80, 25);
		reg_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String user = usernameText.getText();
				String input_word = pass_Text.getText();
				String _email = email_Text.getText();
				String secret_key = user + input_word;
				
				add_user_todb(user, secret_key, _email);
				String QR_code_path = generate_qr(secret_key, user);
				Send_email(_email, user);
			}
		});
		
		//Login page button
		JButton login_button = new JButton("Login");
		login_button.setBounds(130, 120, 80, 25);
		login_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Registration_frame.setVisible(false);
			}
		});
		JLabel _or = new JLabel("OR");
		_or.setBounds(100, 120, 40, 25);
		Registration_panel.add(reg_button);
		Registration_panel.add(_or);
		Registration_panel.add(login_button);
	}
	
	
	//insert user into database
	private void add_user_todb(String user, String secret_key, String _email) {
		String sql_command = "INSERT INTO users VALUES("+"'"+ user +"'"+", "+ "'"+secret_key +"'"+", "+ "'"+_email+"'" +");";
		
		Statement statement = null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		try {
			statement.executeUpdate(sql_command);
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
	}
	
	// fetch user from database using the secret password.
	private void fetch_user_fromdb(String username, String password) {
		String sql_command = "SELECT name FROM users WHERE unique_key='"+ password + "';";
		Statement statement = null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		try {
			ResultSet result = statement.executeQuery(sql_command);
			display_welcome_page(result.getString("name"));
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
	}
	
	
	// if scan worked display welcome page.
	private void display_welcome_page(String username) {
		scanning = false;
		JLabel welcome_message = new JLabel("Welcome "+ username+" You have successfully logged in with your QR code!");
		welcome_message.setBounds(220, 50, 80, 25);
		welcome_panel.add(welcome_message);
		
		frame.remove(Login_panel);
		frame.repaint();
		frame.add(welcome_panel);
		frame.revalidate();
	}
	
	
	// generates a qr code, embedding a string( username + random string).
	private String generate_qr(String secret_key, String user) {
		String path = "";
		String org_code = "Salad_Corp";
		Map<String, String> user_dict = new HashMap<String, String>();
		user_dict.put("password", secret_key);
		user_dict.put("username", user);
		user_dict.put("Organization", org_code);
		String str = user_dict.toString();
				
		try {
			BitMatrix matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, 200, 200);
			path = "/home/gokul/eclipse-workspace/demo/" + user + ".png";
			MatrixToImageWriter.writeToFile(matrix, path.substring(path.lastIndexOf('.') + 1), new File(path));
			return path;
		} catch (WriterException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return path;
	}
	
	
	protected void Send_email(String _email,String user ) {
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "587");
		
		String sender_email = "gokulkrh@gmail.com";
		String email_password = "jngckohplnfdlvie";
		
		Session session = Session.getInstance(properties, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(sender_email, email_password);
			}
		});
		
		Message mail_with_QR = new MimeMessage(session);
		try {
			mail_with_QR.setFrom(new InternetAddress(sender_email));
			mail_with_QR.setRecipient(Message.RecipientType.TO, new InternetAddress(_email));
			mail_with_QR.setSubject("Login QR code");
	        Multipart multipart = new MimeMultipart();
	        
	        MimeBodyPart textBody = new MimeBodyPart();
	        textBody.setText("Please see the attachment below to find your QR code.");
			
			
			// attaching image
			MimeBodyPart messageBodyPart = new MimeBodyPart();
	        String file = user+".png";
	        DataSource source = new FileDataSource(file);
	        messageBodyPart.setDataHandler(new DataHandler(source));
	        messageBodyPart.setFileName(file);
	        
	        multipart.addBodyPart(textBody);
	        multipart.addBodyPart(messageBodyPart);

	        mail_with_QR.setContent(multipart);
	        
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
			Transport.send(mail_with_QR);
			System.out.println("Email sent successfully");
		} catch (MessagingException e1) {
			e1.printStackTrace();
		}
	}
}
