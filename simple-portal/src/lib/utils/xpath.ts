/**
 * XPath evaluation on XML strings.
 */

export function evaluateXpath(xml: string, xpath: string): string | null {
  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xml, 'application/xml');

    const parserError = doc.querySelector('parsererror');
    if (parserError) return null;

    const evaluator = new XPathEvaluator();
    const result = evaluator.evaluate(
      xpath,
      doc,
      null,
      XPathResult.FIRST_ORDERED_NODE_TYPE,
      null,
    );

    const node = result.singleNodeValue;
    return node?.textContent?.trim() || null;
  } catch {
    return null;
  }
}
