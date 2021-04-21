package snytng.astah.plugin.text2model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IElement;

public class ClassReader {
	
	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(ClassReader.class.getName());
	static {
	      ConsoleHandler consoleHandler = new ConsoleHandler();
	      consoleHandler.setLevel(Level.CONFIG);      
	      logger.addHandler(consoleHandler);
	      logger.setUseParentHandlers(false);
	}
		
	private ClassReader(){}
	
	public static String printClass(IClass iClass) {
		StringBuilder text = new StringBuilder("");
		
		String className = iClass.getName();
		IAttribute[] ias = iClass.getAttributes();
		for (int i = 0; i < ias.length; i++) {
			IAttribute ia = ias[i];
			String aName = ia.getName();
			if(aName != null && aName.length() != 0){
				text.append(className + "の" + ia.getName() + " ");
			}
		}
		return text.toString();
	}
	
	public static String printRelations(IClass iClass){
		StringBuilder message = new StringBuilder("");

		List<IElement> elements = new ArrayList<>();
		elements.addAll(Arrays.asList(iClass.getGeneralizations()));
		elements.addAll(Arrays.asList(iClass.getSpecializations()));
		elements.addAll(Arrays.asList(iClass.getSupplierRealizations()));
		elements.addAll(Arrays.asList(iClass.getClientRealizations()));
		
		List<IElement> attrs = Arrays.asList(iClass.getAttributes())
		.stream()
			.map(IAttribute::getAssociation)
			.collect(Collectors.toList());
		elements.addAll(attrs);
		
		for (IElement iElement : elements) {
			String rel = RelationReader.printRelation(iElement);
			if(rel != null){
				message.append(rel + System.lineSeparator());
			}
		}

		return message.toString();
	}
}
