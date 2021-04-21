package snytng.astah.plugin.text2model;

import java.util.List;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;

public interface DiagramMessages {
	public List<String> getMessages(IDiagram diagram, Stream<IElement> elements);
}
