package snytng.astah.plugin.text2model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.model.IClassDiagram;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;

public class ClassDiagramMessages implements DiagramMessages {

	private static ClassDiagramMessages instance = null;

	public static ClassDiagramMessages getInstance(){
		if(instance == null){
			instance = new ClassDiagramMessages();
		}
		return instance;
	}

	private ClassDiagramMessages(){
		// no initialization
	}

	public List<String> getMessages(IDiagram diagram, Stream<IElement> elements){
		IClassDiagram classDiagram = (IClassDiagram)diagram;

		Stream<String> lines = null;

		IElement[] es = elements
				.filter(RelationReader::isSupportedRelation)
				.toArray(IElement[]::new);

		if(es.length != 0){
			// 選択している関連があれば読み上げる
			lines = Arrays.stream(es).map(RelationReader::printRelation);
		} else {
			// クラス図全体の読み上げ
			lines =  new ClassDiagramReader(classDiagram).read().stream();
		}

		// メッセージのカッコ、。を外してListを返却
		return lines
				.map(s -> s.replaceAll("[「」]", ""))
				.map(s -> s.replaceAll("。", ""))
				.collect(Collectors.toList());
	}
}
