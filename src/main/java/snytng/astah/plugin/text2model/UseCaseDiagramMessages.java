package snytng.astah.plugin.text2model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;

public class UseCaseDiagramMessages implements DiagramMessages {
	
	private static UseCaseDiagramMessages instance = null;
	
	public static UseCaseDiagramMessages getInstance(){
		if(instance == null){
			instance = new UseCaseDiagramMessages(); 
		}
		return instance;
	}
	
	private UseCaseDiagramMessages(){
		// no initialization
	}
	
	public List<String> getMessages(IDiagram diagram, Stream<IElement> elements){
		IUseCaseDiagram usecaseDiagram = (IUseCaseDiagram)diagram;
		
		Stream<String> lines = null;

		UseCaseDiagramReader ucdr = new UseCaseDiagramReader(usecaseDiagram);

		IElement[] es = elements
				.filter(ucdr::isSupportedElement)
				.toArray(IElement[]::new);
		
		if(es.length != 0){
			// 選択している関連があれば読み上げる
			lines = Arrays.stream(es).map(ucdr::read);
		} else {
			// ユースケース図全体の読み上げ
			lines =  ucdr.read().stream();
		}
		
		// メッセージのカッコを外してListを返却
		return lines
				.map(s -> s.replaceAll("[「」]", ""))
				.collect(Collectors.toList());
	}
}
