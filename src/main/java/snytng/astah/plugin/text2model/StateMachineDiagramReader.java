package snytng.astah.plugin.text2model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;

public class StateMachineDiagramReader {

	private IStateMachineDiagram diagram = null;

	public StateMachineDiagramReader(IStateMachineDiagram diagram){
		this.diagram = diagram;
	}

	/**
	 * 状態マシン図に含まれる状態数を取得する
	 * @return 状態数
	 */
	public int getNumberOfStates(){
		IStateMachine statemachine = this.diagram.getStateMachine();
		return statemachine == null ? 0 : statemachine.getVertexes().length;
	}

	/**
	 * 状態マシン図に含まれる遷移数を取得する
	 * @return 遷移数
	 */
	public int getNumberOfTransitions(){
		IStateMachine statemachine = this.diagram.getStateMachine();
		return statemachine == null ? 0 : statemachine.getTransitions().length;
	}
	
	public boolean isSupportedElement(IElement e){
		if(e == null){
			return false;
		}
		return (e instanceof ITransition);
	}
	
	public String read(IElement e){
		// 遷移
		if (e instanceof ITransition){
			ITransition t = (ITransition)e;
			String tra = TransitionReader.read(t);
			if(! tra.equals("")){
				return tra;
			}
		}
		return null;
	}

	public List<String> read(){
		List<String> mps = new ArrayList<>();

		IPresentation[] ps;
		try {
			ps = diagram.getPresentations();
			for (IPresentation p : ps){
				if (p.getModel() instanceof ITransition){
					ITransition t = (ITransition)p.getModel();
					String tra = TransitionReader.read(t);
					if(! tra.equals("")){
						mps.add(tra);
					}
				}
			}
		} catch (InvalidUsingException e) {
			e.printStackTrace();
		}
		return mps;
	}

}
