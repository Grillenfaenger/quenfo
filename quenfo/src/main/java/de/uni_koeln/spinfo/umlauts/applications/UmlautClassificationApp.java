package de.uni_koeln.spinfo.umlauts.applications;

public class UmlautClassificationApp {
	
	// Trainieren
	
	// Gruppierte Lesarten + deren Kontexte holen (Dafür reicht der einfache Tokenisierer)
	//Für jede Lesartengruppe Trainingsmodelle erstellen
		// Classification Units erstellen (diese sind dann schon initialisiert)
		// cu.setFeatures()
		// cu.setFeatureVectors()
	// build Modell für alle Gruppen
	// Modelle den Gruppen zugeordnet vorhalten
	
	//Klassifizieren
	
	// Im Jahrgang ohne Umlaute nach umlautambigen Wörtern suchen
	// Je eine Anzeige
	// In Sätze splitten und deren Span festhalten
	// Sätze tokenisieren und die Position der Tokens im Satz festhalten
	// für jeden Fund einzeln: 
		// Kontext extrahieren
		// Klassifizieren: cu erstellen, setFeatures(), setFeatureVectors, das entsprechende Modell auswählen und klassifizieren
	//cu.getSense() mit cu.getContent() vergleichen. Falls identisch kein Handlungsbedarf
	// ansonsten ersetzen
	
	

}
