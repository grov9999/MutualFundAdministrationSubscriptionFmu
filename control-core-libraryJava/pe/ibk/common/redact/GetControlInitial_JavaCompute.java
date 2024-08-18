package pe.ibk.common.redact;

import java.util.Map;
import java.util.stream.IntStream;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbPolicy;
import com.ibm.broker.plugin.MbUserException;

public class GetControlInitial_JavaCompute extends MbJavaComputeNode {


	private final static String METADATA_NEW_RELIC = "metadata";
	private final static String SUFIX_METADATA = "_NAME";
	private final static String METADATA_EMPTY = "ocp";
	private final static String PREFIX_METADATA = "NEW_RELIC_METADATA_KUBERNETES_";
	private final static int LENGTH_PREFIX_METADATA = PREFIX_METADATA.length();
	private final static int LENGTH_SUFIX_METADATA = SUFIX_METADATA.length();
	
	
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below

			String redactPolicyName = (String)getUserDefinedAttribute("RedactPolicyName");
			String redactPolicyProperty = (String)getUserDefinedAttribute("RedactPolicyProperty");
			
			String configPolicyName = (String)getUserDefinedAttribute("ConfigPolicyName");
			String configPolicyProperty = (String)getUserDefinedAttribute("ConfigPolicyProperty");
			
			// ----------------------------------------------------------
			String monitorACFPolicyName = (String)getUserDefinedAttribute("ConfigMonitorACFPolicyName");
			String monitorACFPolicyProperty = (String)getUserDefinedAttribute("ConfigMonitorACFPolicyProperty");
			// ----------------------------------------------------------
			
			MbPolicy redactPolicy = MbPolicy.getPolicy("UserDefined",redactPolicyName);
			String myMsg = redactPolicy.getPropertyValueAsString(redactPolicyProperty);
			
			MbPolicy configPolicy = MbPolicy.getPolicy("UserDefined",configPolicyName);
			String myConfigMsg = configPolicy.getPropertyValueAsString(configPolicyProperty);
			
			String key = System.getenv("KEY");
			String secret = System.getenv("SECRET");
			
			// ----------------------------------------------------------
			String myConfigACFMsg = null;
			String emptyString = "";
			if( !monitorACFPolicyName.equals(emptyString)|| !monitorACFPolicyProperty.equals(emptyString)){
				MbPolicy monitotACFPolicy = MbPolicy.getPolicy("UserDefined",monitorACFPolicyName);
				myConfigACFMsg = monitotACFPolicy.getPropertyValueAsString(monitorACFPolicyProperty);
			}

			String namespace = System.getenv("NEW_RELIC_METADATA_KUBERNETES_NAMESPACE_NAME");
			// ----------------------------------------------------------
			
			MbMessage envMessage = inAssembly.getGlobalEnvironment();
			envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "RedactPolicyValue", myMsg);
			envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "ConfigPolicyValue", myConfigMsg);
			envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "Namespace", namespace);
			// ----------------------------------------------------------
			envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "ConfigMonitorACFValue", myConfigACFMsg);
			envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "key", key);
			envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "secret", secret);
			// ----------------------------------------------------------

			MbElement mbEparameterNewrelic = envMessage.getRootElement().createElementAsFirstChild(MbElement.TYPE_NAME, METADATA_NEW_RELIC, null);

			Map<String, String> envVariable = System.getenv();
			envVariable.entrySet().stream()
				.filter(env -> env.getKey().contains(PREFIX_METADATA))
				.forEach( env -> {
					try {
							mbEparameterNewrelic.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, capitalizeMetadata(env.getKey()), env.getValue());
					} catch (MbException e) {
						e.printStackTrace();
					}
				});
			
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}


    public static String capitalizeMetadata(String name) {
        int start = LENGTH_PREFIX_METADATA;
        int end = 0;

        if (name == null || name.isEmpty()) {
            return METADATA_EMPTY;
        }

        if (name.contains(SUFIX_METADATA)) {
            end = LENGTH_SUFIX_METADATA;
        }

        char charValue[] = name.toCharArray();
        // charValue[start] = Character.toUpperCase(charValue[start]);

        IntStream.range(start + 1, charValue.length - end)
                .forEach(i -> charValue[i] = Character.toLowerCase(charValue[i]));

        int length = charValue.length - start - end;
        return new String(charValue, start, length);
    }
	
	
	/**
	 * onPreSetupValidation() is called during the construction of the node
	 * to allow the node configuration to be validated.  Updating the node
	 * configuration or connecting to external resources should be avoided.
	 *
	 * @throws MbException
	 */
	@Override
	public void onPreSetupValidation() throws MbException {
	}

	/**
	 * onSetup() is called during the start of the message flow allowing
	 * configuration to be read/cached, and endpoints to be registered.
	 *
	 * Calling getPolicy() within this method to retrieve a policy links this
	 * node to the policy. If the policy is subsequently redeployed the message
	 * flow will be torn down and reinitialized to it's state prior to the policy
	 * redeploy.
	 *
	 * @throws MbException
	 */
	@Override
	public void onSetup() throws MbException {
	}

	/**
	 * onStart() is called as the message flow is started. The thread pool for
	 * the message flow is running when this method is invoked.
	 *
	 * @throws MbException
	 */
	@Override
	public void onStart() throws MbException {
	}

	/**
	 * onStop() is called as the message flow is stopped. 
	 *
	 * The onStop method is called twice as a message flow is stopped. Initially
	 * with a 'wait' value of false and subsequently with a 'wait' value of true.
	 * Blocking operations should be avoided during the initial call. All thread
	 * pools and external connections should be stopped by the completion of the
	 * second call.
	 *
	 * @throws MbException
	 */
	@Override
	public void onStop(boolean wait) throws MbException {
	}

	/**
	 * onTearDown() is called to allow any cached data to be released and any
	 * endpoints to be deregistered.
	 *
	 * @throws MbException
	 */
	@Override
	public void onTearDown() throws MbException {
	}

}
