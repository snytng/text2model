package snytng.astah.plugin.text2model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;

public class NoDiagramMessages implements DiagramMessages {

	private static NoDiagramMessages instance = null;

	public static NoDiagramMessages getInstance(){
		if(instance == null){
			instance = new NoDiagramMessages();
		}
		return instance;
	}

	private NoDiagramMessages(){
		// no initialization
	}

	public List<String> getMessages(IDiagram diagram, Stream<IElement> elements){
		return new ArrayList<String>();
	}
}
