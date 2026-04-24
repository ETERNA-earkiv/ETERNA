# PDF-maskning

PDF-maskning låter dig permanent maskera känsligt innehåll i en PDF-fil. De maskerade områdena tas bort ur filen och kan inte återställas.

## Starta en maskningssession

Navigera till den fil du vill maskera och klicka på knappen **Maskera PDF** i verktygsfältet. Knappen är bara tillgänglig för PDF-filer.

Innan maskeringsredigeraren öppnas visas en dialog där du ombeds ange ett skäl till maskningen. Skälet registreras i granskningsloggen tillsammans med ditt användarnamn och tidpunkten för åtgärden.

Ange skälet och klicka på **Bekräfta** för att öppna redigeraren. Klicka på **Avbryt** för att avbryta utan att öppna redigeraren.

> **Observera:** Beroende på systemkonfigurationen kan det vara obligatoriskt att ange ett skäl. När det är obligatoriskt är Bekräfta-knappen inaktiverad tills du har angett ett skäl.

## Använda maskeringsredigeraren

Redigerarens verktygsfält innehåller alla verktyg du behöver för att markera, applicera och spara maskeringar.

### Navigering

Använd knapparna **Föregående sida** och **Nästa sida**, eller skriv ett sidnummer direkt i sidfältet, för att bläddra mellan sidorna. Sidopanelen visar miniatyrbilder av alla sidor och kan visas eller döljas med sidopanelsknappen. Använd zoomväljaren för att justera vyn: välj anpassa till sida, anpassa till bredd eller en fast procentnivå.

### Markera innehåll för maskning

Det finns två verktyg för att markera innehåll:

- **Maskera text** — välj det här verktyget och klicka och dra sedan över text i dokumentet för att markera den för maskning. Angränsande textrader slås automatiskt ihop till en enda markering.
- **Maskera område** — välj det här verktyget och klicka och dra sedan var som helst på sidan för att rita en rektangel över det innehåll du vill maskera.

Markerade områden visas med en färgad överlagring. Markeringarna är inte permanenta i det här skedet.

### Applicera markeringar

Klicka på **Applicera** för att permanent bekräfta de aktuella markeringarna som maskeringar. Applicerade maskeringar visas som solida svarta rektanglar i redigeraren.

Du kan fortsätta markera och applicera på andra sidor innan du sparar.

### Ångra och göra om

- **Ångra** — går tillbaka ett steg i historiken för applicerade maskeringsgrupper.
- **Gör om** — återapplicerar tidigare ångrade maskeringsgrupper.

### Rensa alla maskeringar

Klicka på knappen **Rensa alla maskeringar** (cirkulär pil) för att ta bort alla markeringar och applicerade maskeringar. En bekräftelsedialog visas innan något tas bort.

### Spara

När du är klar klickar du på **Spara**. En dialog ber dig bekräfta innan exporten påbörjas. Redigeraren renderar varje sida och ersätter de maskerade områdena med solida svarta rektanglar i den exporterade filen. En förloppsindikator visar hur många sidor som har bearbetats. När exporten är klar sparas den maskerade filen tillbaka till arkivet.

## Granskningslogg

Varje gång maskeringsredigeraren öppnas skapas en post i granskningsloggen som innehåller:

- användaren som initierade maskningen
- filen som maskerades
- datum och tidpunkt
- det angivna skälet

Detta säkerställer att all maskeringsaktivitet är spårbar och kan granskas i efterhand.
