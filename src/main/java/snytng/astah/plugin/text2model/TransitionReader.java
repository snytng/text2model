package snytng.astah.plugin.text2model;

import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;

public class TransitionReader {
	
	private TransitionReader(){}

	public static boolean isSupportedTransition(IElement e){
		if(e == null) {
			return false;
		} else {
			return 	(e instanceof ITransition);
		}
	}


	public static String read(ITransition iTransition){
		String message ="○,";

		IVertex sourceState = iTransition.getSource();
		String source       = sourceState.getName();
		IVertex targetState = iTransition.getTarget();
		String target       = targetState.getName();
		
		String event = iTransition.getEvent();
		String action = iTransition.getAction();
		String guard = iTransition.getGuard();
		String doActivity = null;
		
		if(iTransition.getSource() instanceof IState){
			doActivity = ((IState)iTransition.getSource()).getDoActivity();
		}

		// 元状態が疑似状態の場合
		if(sourceState instanceof IPseudostate){
			// 疑似開始状態の場合
			if(((IPseudostate) sourceState).isInitialPseudostate()){
				IPseudostate initialState = (IPseudostate)sourceState;
				// 状態遷移図の疑似開始状態の場合
				if(initialState.getContainer() == null){
					message += "最初,は、,";
				} 
				// ある状態の疑似開始状態の場合
				else {
					message += initialState.getContainer() + "の最初,は、,";
				}
			}
			// 疑似開始状態以外の場合
			else {
				message += source + ",は、,";
			}
			message += ",";
		}
		//元状態が疑似状態以外の場合
		else {
			message += source + ",は、,";

			if(guard != null && ! guard.equals("")){
				message += "[" +guard + "]"; 
			}
			
			if(event != null && ! event.equals("")){
				message += event + "で,";
			} else {
				message += ","; // ヌル遷移
			}
		}
		
		message += target + "に遷移する";

		return message;
	}
}
