package snytng.astah.plugin.text2model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;

public class StateMachineDiagramMessages implements DiagramMessages {
	
	private static StateMachineDiagramMessages instance = null;
	
	public static StateMachineDiagramMessages getInstance(){
		if(instance == null){
			instance = new StateMachineDiagramMessages(); 
		}
		return instance;
	}
	
	private StateMachineDiagramMessages(){
		// no initialization
	}
	
	public List<String> getMessages(IDiagram diagram, Stream<IElement> elements){
		IStateMachineDiagram stateMachineDiagram = (IStateMachineDiagram)diagram;
		
		Stream<String> lines = null;

		StateMachineDiagramReader stdr = new StateMachineDiagramReader(stateMachineDiagram);

		IElement[] es = elements
				.filter(stdr::isSupportedElement)
				.toArray(IElement[]::new);
		
		if(es.length != 0){
			// 選択している関連があれば読み上げる
			lines = Arrays.stream(es).map(stdr::read);
		} else {
			// ユースケース図全体の読み上げ
			lines =  stdr.read().stream();
		}
		
		// メッセージのカッコを外してListを返却
		return lines
				.map(s -> s.replaceAll("[「」]", ""))
				.collect(Collectors.toList());
	}
}
