package org.exist.security.xacml;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicyReference;
import com.sun.xacml.PolicySet;
import com.sun.xacml.PolicyTreeElement;
import com.sun.xacml.ProcessingException;
import com.sun.xacml.Target;
import com.sun.xacml.cond.Apply;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.PolicyFinderResult;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.exist.collections.Collection;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.NodeSet;
import org.exist.dom.QName;
import org.exist.storage.DBBroker;
import org.exist.storage.NativeValueIndexByQName;
import org.exist.storage.UpdateListener;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.AnyURIValue;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.Sequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class contains utility methods for working with XACML
 * in eXist.
 */
public class XACMLUtil implements UpdateListener
{
	private static final Logger LOG = Logger.getLogger(ExistPolicyModule.class);
	private static final Map POLICY_CACHE = Collections.synchronizedMap(new HashMap(8));
	
	private ExistPDP pdp;
	
	private XACMLUtil() {}
	XACMLUtil(ExistPDP pdp)
	{
		if(pdp == null)
			throw new NullPointerException("ExistPDP cannot be null");
		this.pdp = pdp;
		pdp.getBrokerPool().getNotificationService().subscribe(this);
	}

	//UpdateListener method
	/**
	 * This method is called by the <code>NotificationService</code>
	 * when documents are updated in the databases.  If a document
	 * is removed or updated from the policy collection, it is removed
	 * from the policy cache.
	 */
	public void documentUpdated(DocumentImpl document, int event)
	{
		if(inPolicyCollection(document) && (event == UpdateListener.REMOVE || event == UpdateListener.UPDATE))
			POLICY_CACHE.remove(document.getName());
	}
	/**
	 * Returns true if the specified document is in the policy collection.
	 * This does not check subcollections.
	 * 
	 * @param document The document in question
	 * @return if the document is in the policy collection
	 */
	public static boolean inPolicyCollection(DocumentImpl document)
	{
		return XACMLConstants.POLICY_COLLECTION.equals(document.getCollection().getName());
	}
	/**
	* Performs any necessary cleanup operations.  Generally only
	* called if XACML has been disabled.
	*/
	public void close()
	{
		pdp.getBrokerPool().getNotificationService().unsubscribe(this);
	}
	
	/**
	* Gets the policy (or policy set) specified by the given id.
	* 
	* @param type The type of id reference:
	*	PolicyReference.POLICY_REFERENCE for a policy reference
	*	or PolicyReference.POLICYSET_REFERENCE for a policy set
	*	reference.
	* @param idReference The id of the policy (or policy set) to
	*	retrieve
	* @param broker the broker to use to access the database
	* @return The referenced policy.
	* @throws ProcessingException if there is an error finding
	*	the policy (or policy set).
	* @throws XPathException
	*/
	public AbstractPolicy findPolicy(DBBroker broker, URI idReference, int type) throws ParsingException, ProcessingException, XPathException
	{
		QName idAttributeQName = getIdAttributeQName(type);
		if(idAttributeQName == null)
			throw new NullPointerException("Invalid reference type: " + type);
			
		DocumentImpl policyDoc = getPolicyDocument(broker, idAttributeQName, idReference);
		if(policyDoc == null)
			return null;
			
		return getPolicyDocument(policyDoc);
	}
	
	/**
	* This method returns all policy documents in the policies collection.
	* If recursive is true, policies in subcollections are returned as well.
	*
	* @param broker the broker to use to access the database
	* @param recursive true if policies in subcollections should be
	*	returned as well
	* @return All policy documents in the policies collection
	*/
	public static DocumentSet getPolicyDocuments(DBBroker broker, boolean recursive)
	{
		Collection policyCollection = broker.getCollection(XACMLConstants.POLICY_COLLECTION);
		if(policyCollection == null)
		{
			LOG.warn("Policy collection '" + XACMLConstants.POLICY_COLLECTION + "' does not exist");
			return null;
		}

		int documentCount = policyCollection.getDocumentCount();
		if(documentCount == 0)
		{
			LOG.warn("Policy collection contains no documents.");
			return null;
		}
		DocumentSet documentSet = new DocumentSet(documentCount);
		return policyCollection.allDocs(broker, documentSet, recursive, false);
	}
	/**
	* Returns the single policy (or policy set) document that has the
	* attribute specified by attributeQName with the value
	* attributeValue, null if none match, or throws a
	* <code>ProcessingException</code> if more than one match.  This is
	* performed by a QName range index lookup and so it requires a range
	* index to be given on the attribute.
	* 
	* @param attributeQName The name of the attribute
	* @param attributeValue The value of the attribute
	* @param broker the broker to use to access the database
	* @return The referenced policy.
	* @throws ProcessingException if there is an error finding
	*	the policy (or policy set) documents.
	* @throws XPathException if there is an error performing
	*	the index lookup
	*/
	public DocumentImpl getPolicyDocument(DBBroker broker, QName attributeQName, URI attributeValue) throws ProcessingException, XPathException
	{
		DocumentSet documentSet = getPolicyDocuments(broker, attributeQName, attributeValue);
		int documentCount = (documentSet == null) ? 0 : documentSet.getLength();
		if(documentCount == 0)
		{
			LOG.warn("Could not find " + attributeQName.getLocalName() + " '" +  attributeValue + "'", null);
			return null;
		}

		if(documentCount > 1)
		{
			throw new ProcessingException("Too many applicable policies for " + attributeQName.getLocalName() + " '" +  attributeValue + "'");
		}

		return (DocumentImpl)documentSet.iterator().next();
	}
	/**
	* Gets all policy (or policy set) documents that have the
	* attribute specified by attributeQName with the value
	* attributeValue.  This is performed by a QName range index
	* lookup and so it requires a range index to be given
	* on the attribute.
	* 
	* @param attributeQName The name of the attribute
	* @param attributeValue The value of the attribute
	* @param broker the broker to use to access the database
	* @return The referenced policy.
	* @throws ProcessingException if there is an error finding
	*	the policy (or policy set) documents.
	* @throws XPathException if there is an error performing the
	*	index lookup
	*/
	public DocumentSet getPolicyDocuments(DBBroker broker, QName attributeQName, URI attributeValue) throws ProcessingException, XPathException
	{
		if(attributeQName == null)
			return null;
		if(attributeValue == null)
			return null;
		AtomicValue comparison = new AnyURIValue(attributeValue);

		DocumentSet documentSet = getPolicyDocuments(broker, true);
		NodeSet nodeSet = documentSet.toNodeSet();

		NativeValueIndexByQName index = broker.getQNameValueIndex();
		Sequence results = index.findByQName(attributeQName, comparison, nodeSet);

		return (results == null) ? null : results.getDocumentSet();
	}
	/**
	* Gets the name of the attribute that specifies the policy
	* (if type == PolicyReference.POLICY_REFERENCE) or
	* the policy set (if type == PolicyReference.POLICYSET_REFERENCE).
	*
	* @param type The type of id reference:
	*	PolicyReference.POLICY_REFERENCE for a policy reference
	*	or PolicyReference.POLICYSET_REFERENCE for a policy set
	*	reference.
	* @return The attribute name for the reference type
	*/
	public static QName getIdAttributeQName(int type)
	{
		if(type == PolicyReference.POLICY_REFERENCE)
			return new QName(XACMLConstants.POLICY_ID_LOCAL_NAME, XACMLConstants.XACML_POLICY_NAMESPACE);
		else if(type == PolicyReference.POLICYSET_REFERENCE)
			return new QName(XACMLConstants.POLICY_SET_ID_LOCAL_NAME, XACMLConstants.XACML_POLICY_NAMESPACE);
		else
			return null;
	}
	//logs the specified message and exception
	//then, returns a result with status Indeterminate and the given message
	/**
	* Convenience method for errors occurring while processing.  The message
	* and exception are logged and a <code>PolicyFinderResult</code> is
	* generated with Status.STATUS_PROCESSING_ERROR as the error condition
	* and the message as the message.
	*
	* @param message The message describing the error.
	* @param t The cause of the error, may be null
	* @return A <code>PolicyFinderResult</code> representing the error.
	*/
	public static PolicyFinderResult errorResult(String message, Throwable t)
	{
		LOG.warn(message, t);
		return new PolicyFinderResult(new Status(Collections.singletonList(Status.STATUS_PROCESSING_ERROR), message));
	}

	/**
	* Obtains a parsed representation of the specified XACML Policy or PolicySet
	* document.  If the document has already been parsed, this method returns the
	* cached <code>AbstractPolicy</code>.  Otherwise, it unmarshals the document into
	* an <code>AbstractPolicy</code> and caches it.
	*
	* @param policyDoc the policy (or policy set) document
	*	for which a parsed representation should be obtained
	* @return a parsed policy (or policy set)
	* @throws ParsingException if an error occurs while parsing the specified document
	*/
	public AbstractPolicy getPolicyDocument(DocumentImpl policyDoc) throws ParsingException
	{
		String name = policyDoc.getName();
		AbstractPolicy policy = (AbstractPolicy)POLICY_CACHE.get(name);
		if(policy == null)
		{
			policy = parsePolicyDocument(policyDoc);
			POLICY_CACHE.put(name, policy);
		}
		return policy;
	}
	/**
	* Parses a DOM representation of a policy document into an
	* <code>AbstractPolicy</code>.
	*
	* @param policyDoc The DOM <code>Document</code> representing
	*	the XACML policy or policy set.
	* @return The parsed policy
	* @throws ParsingException if there is an error parsing the document
	*/
	public AbstractPolicy parsePolicyDocument(Document policyDoc) throws ParsingException
	{
		Element root = policyDoc.getDocumentElement();
		String name = root.getTagName();

		if(name.equals(XACMLConstants.POLICY_SET_ELEMENT_LOCAL_NAME))
			return PolicySet.getInstance(root, pdp.getPDPConfig().getPolicyFinder());
		else if(name.equals(XACMLConstants.POLICY_ELEMENT_LOCAL_NAME))
			return Policy.getInstance(root);
		else
			throw new ParsingException("The root element of the policy document must be '" + XACMLConstants.POLICY_SET_ID_LOCAL_NAME + "' or '" + XACMLConstants.POLICY_SET_ID_LOCAL_NAME + "', was: '" + name + "'");
	}
	
	/**
	 * Escapes characters that are not allowed in various places
	 * in XML by replacing all invalid characters with
	 * <code>getEscape(c)</code>.
	 * 
	 * @param buffer The <code>StringBuffer</code> containing
	 * the text to escape in place.
	 */
	public static void XMLEscape(StringBuffer buffer)
	{
		if(buffer == null)
			return;
		char c;
		String escape;
		for(int i = 0; i < buffer.length();)
		{
			c = buffer.charAt(i);
			escape = getEscape(c);
			if(escape == null)
				i++;
			else
			{
				buffer.replace(i, i+1, escape);
				i += escape.length();
			}
		}
	}
	/**
	 * Escapes characters that are not allowed in various
	 * places in XML.  Characters are replaced by the
	 * corresponding entity.  The characters &amp;, &lt;,
	 * &gt;, &quot;, and &apos; are escaped.
	 * 
	 * @param c The character to escape.
	 * @return A <code>String</code> representing the
	 * 	escaped character or null if the character does
	 *  not need to be escaped.
	 */
	public static String getEscape(char c)
	{
		switch(c)
		{
			case '&': return "&amp;";
			case '<': return "&lt;";
			case '>': return "&gt;";
			case '\"': return "&quot;";
			case '\'': return "&apos;";
			default: return null;
		}
	}
	/**
	 * Escapes characters that are not allowed in various places
	 * in XML by replacing all invalid characters with
	 * <code>getEscape(c)</code>.
	 * 
	 * @param in The <code>String</code> containing
	 * the text to escape in place.
	 */
	public static String XMLEscape(String in)
	{
		if(in == null)
			return null;
		StringBuffer temp = new StringBuffer(in);
		XMLEscape(temp);
		return temp.toString();
	}
	
	/**
	 * Serializes the specified <code>PolicyTreeElement</code> to a
	 * <code>String</code> as XML.  The XML is indented if indent
	 * is true.
	 * 
	 * @param element The <code>PolicyTreeElement</code> to serialize
	 * @param indent If the XML should be indented
	 * @return The XML representation of the element
	 */
	public static String serialize(PolicyTreeElement element, boolean indent)
	{
		if(element == null)
			return "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if(indent)
			element.encode(out, new Indenter());
		else
			element.encode(out);
		return out.toString();
	}
	/**
	 * Serializes the specified <code>Target</code> to a
	 * <code>String</code> as XML.  The XML is indented if indent
	 * is true.
	 * 
	 * @param target The <code>Target</code> to serialize
	 * @param indent If the XML should be indented
	 * @return The XML representation of the target
	 */
	public static String serialize(Target target, boolean indent)
	{
		if(target == null)
			return "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if(indent)
			target.encode(out, new Indenter());
		else
			target.encode(out);
		return out.toString();
	}
	/**
	 * Serializes the specified <code>Apply</code> to a
	 * <code>String</code> as XML.  The XML is indented if indent
	 * is true.
	 * 
	 * @param apply The <code>Apply</code> to serialize
	 * @param indent If the XML should be indented
	 * @return The XML representation of the apply
	 */
	public static String serialize(Apply apply, boolean indent)
	{
		if(apply == null)
			return "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if(indent)
			apply.encode(out, new Indenter());
		else
			apply.encode(out);
		return out.toString();
	}
}