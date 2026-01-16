# AI Workflow Dokumentácia

**Meno:** Martin Mikus

**Dátum začiatku:** 16.01.2026

**Dátum dokončenia:** 

**Zadanie:** Backend

---

## 1. Použité AI Nástroje

Vyplň približný čas strávený s každým nástrojom:

- [ ] **Cursor IDE:** _____ hodín
- [x] **Claude Code:** 1 hodín  
- [ ] **GitHub Copilot:** _____ hodín
- [ ] **ChatGPT:** _____ hodín
- [ ] **Claude.ai:** _____ hodín
- [ ] **Iné:** 

**Celkový čas vývoja (priližne):** _____ hodín

---

## 2. Zbierka Promptov

> 💡 **Tip:** Kopíruj presný text promptu! Priebežne dopĺňaj po každej feature.

### Prompt #1: /init

**Nástroj:** Claude Code  
**Kontext:** Setup projektu

**Prompt:**
```
/init
```

**Výsledok:**  
✅ Fungoval perfektne (first try)

**Čo som musel upraviť / opraviť:**
```
```

**Poznámky / Learnings:**
```
```
undo changing java from 21 to 17 and go back to 21
jdk 21 is installed continue. And do not change the java version in project
### Prompt #2: /enhance-initial, /generate-prp, /execute-prp

**Nástroj:** Claude Code 
**Kontext:** Vygenerovanie PRP a spustenie

**Prompt:**
```
/enhance-initial, /generate-prp, /execute-prp
```

**Výsledok:**  
```
Claude upravil moj initial.md subor a pripravil celkom dobre PRP
```
**Úpravy:**
```
```

**Poznámky:**
```
Pri spusteni execute-prp mal problem s java verziou. Nastavil som projektu verziu 21
ale nevedel najst verzio openjdk pretoze v PC som mal nejaku ms-openjdk a mal s tym problem.
Tak sa snazil spustit download ale neuspesne. A nakoniec zmenil java verziu na 17 a tam som
ho stopol. 
```

### Prompt #3: undo changing java from 21 to 17 and go back to 21


**Nástroj:** Claude Code  
**Kontext:** Oprava chyby ktoru sposobil

**Prompt:**
```
undo changing java from 21 to 17 and go back to 21
```

**Výsledok:**  
[ ] ✅ Fungoval perfektne (first try)

**Čo som musel upraviť / opraviť:**
```
Vysledok bol v poriadku nastavil vsetko spat na java 21 ale stale mal problem
s ms-openjdk verziou a musel som ju rucne nainstalovat cez CMD
```

**Poznámky / Learnings:**
```
```

### Prompt #3: jdk 21 is installed continue. And do not change the java version in project

**Nástroj:** Claude Code  
**Kontext:** Oprava chyby ktoru sposobil

**Prompt:**
```
undo changing java from 21 to 17 and go back to 21
```

**Výsledok:**  
[ ] ✅ Fungoval perfektne (first try)

**Čo som musel upraviť / opraviť:**
```
Nakoniec som musel nainstalovat rucne openjdk 21 verziu lebo stale sa cyklil a chcel zmenit
verziu na java 17. Pravdepodobne preto lebo som mal nainstalovanu openjdk 17.
```

**Poznámky / Learnings:**
```
Asi by som mu explicitne povedal v user-module.md ze nemen java verziu a pouzi existujucu ms-openjdk-21.
```

### Prompt #4: run next step

**Nástroj:** Claude Code  
**Kontext:** Dokoncenie PRP

**Prompt:**
```
run next step
```

**Výsledok:**  
[ ] ✅ Fungoval perfektne (first try)  

**Čo som musel upraviť / opraviť:**
```
```

**Poznámky / Learnings:**
```
Dokoncil PRP v poriadku bez problemov aj s testami.
```

### Prompt #5: Lepsie usporiadanie package-ov

**Nástroj:** Claude Code  
**Kontext:** Uprava package-ov

**Prompt:**
```
move the @symbol:GlobalExceptionHandler  to the new package com.example.api.error
move the package com.example.zadanie.controller and com.example.zadanie.dto to the com.example.zadanie.api
```

**Výsledok:**  
[ ] ✅ Fungoval perfektne (first try)

**Čo som musel upraviť / opraviť:**
```
```

**Poznámky / Learnings:**
```
Keby to robim od zaciatku tak mu hned definujem ako chcem aby vyzeral projektovy strom.
```
---

## 3. Problémy a Riešenia 

> 💡 **Tip:** Problémy sú cenné! Ukazujú ako riešiš problémy s AI.

### Problém #1: _________________________________

**Čo sa stalo:**
```
[Detailný popis problému - čo nefungovalo? Aká bola chyba?]
```

**Prečo to vzniklo:**
```
[Tvoja analýza - prečo AI toto vygeneroval? Čo bolo v prompte zlé?]
```

**Ako som to vyriešil:**
```
[Krok za krokom - čo si urobil? Upravil prompt? Prepísal kód? Použil iný nástroj?]
```

**Čo som sa naučil:**
```
[Konkrétny learning pre budúcnosť - čo budeš robiť inak?]
```

**Screenshot / Kód:** [ ] Priložený

---

### Problém #2: _________________________________

**Čo sa stalo:**
```
```

**Prečo:**
```
```

**Riešenie:**
```
```

**Learning:**
```
```

## 4. Kľúčové Poznatky

### 4.1 Čo fungovalo výborne

**1.** 
```
[Príklad: Claude Code pre OAuth - fungoval first try, zero problémov]
```

**2.** 
```
```

**3.** 
```
```

**[ Pridaj viac ak chceš ]**

---

### 4.2 Čo bolo náročné

**1.** 
```
[Príklad: Figma MCP spacing - často o 4-8px vedľa, musel som manuálne opravovať]
```

**2.** 
```
```

**3.** 
```
```

---

### 4.3 Best Practices ktoré som objavil

**1.** 
```
[Príklad: Vždy špecifikuj verziu knižnice v prompte - "NextAuth.js v5"]
```

**2.** 
```
```

**3.** 
```
```

**4.** 
```
```

**5.** 
```
```

---

### 4.4 Moje Top 3 Tipy Pre Ostatných

**Tip #1:**
```
[Konkrétny, actionable tip]
```

**Tip #2:**
```
```

**Tip #3:**
```
```

---

## 6. Reflexia a Závery

### 6.1 Efektivita AI nástrojov

**Ktorý nástroj bol najužitočnejší?** _________________________________

**Prečo?**
```
```

**Ktorý nástroj bol najmenej užitočný?** _________________________________

**Prečo?**
```
```

---

### 6.2 Najväčšie prekvapenie
```
[Čo ťa najviac prekvapilo pri práci s AI?]
```

---

### 6.3 Najväčšia frustrácia
```
[Čo bolo najfrustrujúcejšie?]
```

---

### 6.4 Najväčší "AHA!" moment
```
[Kedy ti došlo niečo dôležité o AI alebo o developmente?]
```

---

### 6.5 Čo by som urobil inak
```
[Keby si začínal znova, čo by si zmenil?]
```

### 6.6 Hlavný odkaz pre ostatných
```
[Keby si mal povedať jednu vec kolegom o AI development, čo by to bylo?]
```
