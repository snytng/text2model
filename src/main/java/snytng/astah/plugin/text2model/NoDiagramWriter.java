package snytng.astah.plugin.text2model;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoDiagramWriter implements DiagramWriter {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(NoDiagramWriter.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private NoDiagramWriter() {
	}

	private static NoDiagramWriter instance = null;

	public static NoDiagramWriter getInstance(){
		if(instance == null){
			instance = new NoDiagramWriter();
		}
		return instance;
	}

	public Supplier<String> getModeGetter(){
		return () -> "未対応";
	}

	public void setNounMode(boolean b){
	}

	public Function<String, String[][]> getTextAanalyzer(){
		return str -> new String[][] {};
	}

	public void setSequenceDiagramMode(boolean b){
	}

	public Consumer<FunctionCreator> getFunctionVisualizer(){
		return f -> {};
	}

}
