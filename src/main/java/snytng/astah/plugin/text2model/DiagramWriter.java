package snytng.astah.plugin.text2model;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DiagramWriter {

	public void setNounMode(boolean b);
	
	public void setSequenceDiagramMode(boolean b);
	
	public Supplier<String> getModeGetter();
	
	public Function<String, String[][]> getTextAanalyzer();
	
	public Consumer<FunctionCreator> getFunctionVisualizer();	
}
