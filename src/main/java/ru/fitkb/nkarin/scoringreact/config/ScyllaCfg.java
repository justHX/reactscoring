package ru.fitkb.nkarin.scoringreact.config;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.FallthroughRetryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.core.cql.session.DefaultBridgedReactiveSession;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ScyllaCfg {

	@Value("#{'${scylla.dcHosts}'.split(',')}")
	private List<String> servers;

	@Value("${scylla.reconnect.baseDelay:500}")
	private Integer reconnectBaseDelay;

	@Value("${scylla.reconnect.maxDelay:300000}")
	private Integer reconnectMaxDelay;

	@Value("${scylla.socket.connectTimeoutMs:5000}")
	private Integer socketConnectTimeoutMs;

	@Value("${scylla.socket.readTimeoutMs:12000}")
	private Integer socketReadTimeoutMs;

	@Value("${scylla.keyspace}")
	private String keyspace;

	@Value("${scylla.username}")
	private String dbUserName;

	@Value("${scylla.password}")
	private String dbPassword;

	/**
	 * Cassandra cluster class.
	 *
	 * @return Cluster
	 */
	@Bean
	public Cluster cluster() {

		Cluster.Builder clusterBuilder = Cluster.builder();

		/*
		 * Hosts should be in order of closest hosts (same DC) to farthest (remote DC).
		 *
		 * Max of 2-4 hosts hosts per DC are needed as the first one connected to will
		 * be used to determine all the clusters nodes and DCs
		 */
		List<InetSocketAddress> collect = servers.stream().map(i -> {
			String[] split = i.split(":");
			return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
		}).collect(Collectors.toList());

		clusterBuilder.addContactPointsWithPorts(collect);

		/*
		 * Start reconnect at 500ms and exponentially increase to 5 mins before stopping
		 * Looks like: 500ms -> 1s -> 2s -> 4s -> 8s -> 16s -> 32s -> 1m4s -> 2m8s ->
		 * 4m16s -> stop
		 */
		clusterBuilder.withReconnectionPolicy(new ExponentialReconnectionPolicy(reconnectBaseDelay,
				reconnectMaxDelay));

		/*
		 * Set connection/read timeouts
		 *
		 * ConnectTimeoutMillis = 5seconds = default
		 *
		 * ReadTimeoutMillis = reduce this if needing to address lower SLAs than 12s,
		 * but expect higher number of timeouts it should be higher than the timeout
		 * settings used on the Cassandra side
		 */
		SocketOptions socketOptions = new SocketOptions().setConnectTimeoutMillis(socketConnectTimeoutMs)
				.setReadTimeoutMillis(socketReadTimeoutMs);
		clusterBuilder.withSocketOptions(socketOptions);
		clusterBuilder.withCredentials(dbUserName, dbPassword);

		/*
		 * Bubble up all exceptions from the driver to the app to chose what
		 * direction/action should be taken
		 */
		clusterBuilder.withRetryPolicy(FallthroughRetryPolicy.INSTANCE);

		return clusterBuilder.withoutJMXReporting().build();
	}

	@Bean
	public ReactiveSession reactiveSession(Cluster cluster) {
		Session session = cluster.connect(keyspace);

		return new DefaultBridgedReactiveSession(session);
	}

}
