package snytng.astah.plugin.text2model;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IDependency;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IGeneralization;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IRealization;
import com.change_vision.jude.api.inf.presentation.IPresentation;

public class RelationReader {

	private RelationReader(){}

	/**
	 * 読み上げをサポートされている関連かどうかを返却する
	 * @param e モデル要素
	 * @return サポートされている場合はtrue、サポートされていない場合にはfalse
	 */
	public static boolean isSupportedRelation(IElement e){
		if(e == null) return false;

		return
				(e instanceof IAssociation) ||
				(e instanceof IDependency)  ||
				(e instanceof IRealization) ||
				(e instanceof IGeneralization);
	}

	/**
	 * 関連を読み上げる
	 * @param e モデル要素
	 * @return 関連を読み上げた文字列、読み上げられない場合にはnull
	 */
	public static String printRelation(IElement e){
		// 関連
		if(e instanceof IAssociation){
			IAssociation ia = (IAssociation)e;
			return printAssotication(ia);
		}
		// 依存
		else if(e instanceof IDependency){
			IDependency id = (IDependency)e;
			return printDependency(id);
		}
		// 継承
		else if(e instanceof IGeneralization){
			IGeneralization ig = (IGeneralization)e;
			return printGeneralization(ig);
		}
		// 実現
		else if(e instanceof IRealization){
			IRealization ir = (IRealization)e;
			return printRealization(ir);
		}
		// それ以外
		else {
			return null;
		}
	}

	public static String printDependency(IDependency id) {
		INamedElement client = id.getClient();
		INamedElement supplier = id.getSupplier();
		return String.format("○,「%s」,は、,「%s」,を使う。", client, supplier);
	}

	public static String printGeneralization(IGeneralization ig) {
		IClass sub = ig.getSubType();
		IClass sup = ig.getSuperType();
		return String.format("○,「%s」,は、,「%s」,の一種だ。", sub, sup);
	}

	public static String printRealization(IRealization iRealization) {
		INamedElement client = iRealization.getClient();
		INamedElement supplier = iRealization.getSupplier();
		return String.format("○,「%s」,は、,「%s」,の実現だ。", client, supplier);
	}

	public static String printAssotication(IAssociation iAssociation) {
		String ox = "";

		// 関連名の読み方の方向＝▲の方向
		// IPresentationのname_direction_reverseが0なら関連の方向と同じ、1ながら関連の方向と反対
		boolean direction = true;
		try {
			IPresentation[] ips = iAssociation.getPresentations();
			direction = ips[0].getProperty("name_direction_reverse").equals("0");
		}catch(InvalidUsingException e){
			direction = false;
		}

		IAttribute[] iAttributes = iAssociation.getMemberEnds();
		IAttribute fromAttribute = iAttributes[0];
		IAttribute toAttribute = iAttributes[1];

		// 関連名
		String verb = iAssociation.getName();

		// 関連名がない場合
		if(verb.isEmpty()){
			ox = "×";
			// 集約
			if (iAttributes[0].isAggregate() || iAttributes[0].isComposite()) {
				fromAttribute = iAttributes[1];
				toAttribute = iAttributes[0];
				ox = "○";
				verb = "の一部だ";

			}
			// 集約
			else if(iAttributes[1].isAggregate() || iAttributes[1].isComposite()){
				fromAttribute = iAttributes[0];
				toAttribute = iAttributes[1];
				ox = "○";
				verb = "の一部だ";
			}
		}
		// 関連名がある場合
		else {
			ox = "○";
			// 順方向
			if(direction){
				fromAttribute = iAttributes[0];
				toAttribute = iAttributes[1];
			}
			// 逆方向
			else {
				fromAttribute = iAttributes[1];
				toAttribute = iAttributes[0];
			}
		}

		// fromとtoのクラスを決める
		IClass fromClass = fromAttribute.getType();
		IClass toClass = toAttribute.getType();

		// 関連端のロールを取得する
		String fromRole = fromAttribute.getName();
		String toRole = toAttribute.getName();

		// 読み上げの名前を決める
		String fromName = fromClass.toString();
		if(fromRole != null && !fromRole.isEmpty()){
			fromName += "(" + fromRole + ")";
		}
		String toName = toClass.toString();
		if(toRole != null && !toRole.isEmpty()){
			toName += "(" + toRole + ")";
		}

		// 読み上げ文章を作成
		return String.format("%s,「%s」,は、,「%s」,%s。", ox, fromName, toName, verb);
	}
}
