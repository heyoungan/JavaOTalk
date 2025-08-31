module JavaOHTALK_1 {
	requires javafx.controls;
	requires java.sql;
	requires javafx.fxml;
	requires javafx.graphics;
	requires org.json;
	
	exports client;
	opens client to javafx.fxml;
	
	
}
