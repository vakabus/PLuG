#include <assert.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>

#include <pthread.h>

#include "common.h"
#include "connpool.h"
#include "connection.h"
#include "msgchannel.h"


/**
 * Address info for the DiSL server.
 */
static struct addrinfo * disl_addrinfo;

/**
 * Pool of connections to DiSL server.
 */
static struct connection_pool disl_connections;

/**
 * Mutex to protect the manipulation with the connection pool.
 */
static pthread_mutex_t disl_connections_mutex;


static void
__connection_close_hook (struct connection * conn) {
	//
	// Send an empty message to the server before closing the connection
	// to indicate end of processing for that connection.
	//
	struct message shutdown = create_message (0, NULL, 0, NULL, 0);
	send_message (conn, &shutdown);
	free_message (&shutdown);
}


/**
 * Initializes the address info, the pool of connections to the remote
 * instrumentation server, and a mutex guarding the pool.
 */
void
network_init (const char * host_name, const char * port_number) {
	assert (host_name != NULL);
	assert (port_number != NULL);

	pthread_mutex_init (& disl_connections_mutex, NULL);

	struct addrinfo hints;
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_flags = AI_NUMERICSERV | AI_CANONNAME;
	hints.ai_protocol = 0;

	int gai_result = getaddrinfo (host_name, port_number, &hints, &disl_addrinfo);
	check_error (gai_result != 0, gai_strerror (gai_result));

	connection_pool_init (& disl_connections, disl_addrinfo);
	connection_pool_set_before_close_hook (& disl_connections, __connection_close_hook);
}


void
network_fini () {
	//
	// Shut down and close all connections to the server. This has to run
	// under lock, because the connection pool will manipulate the list of
	// connections.
	//
	pthread_mutex_lock (& disl_connections_mutex);
	{
		connection_pool_close (& disl_connections);
	}
	pthread_mutex_unlock (& disl_connections_mutex);

	freeaddrinfo (disl_addrinfo);
	disl_addrinfo = NULL;

	pthread_mutex_destroy (& disl_connections_mutex);
}


struct connection *
network_acquire_connection () {
#ifdef DEBUG
	printf("Acquiring connection ... ");
#endif

	//
	// The connection pool must be protected by a lock so that multiple threads
	// acquiring a connection do not corrupt the internal state of the pool.
	//
	struct connection * connection;
	pthread_mutex_lock (& disl_connections_mutex);
	{
		connection = connection_pool_get_connection (& disl_connections);
	}
	pthread_mutex_unlock (& disl_connections_mutex);


#ifdef DEBUG
	printf("done\n");
#endif

	return connection;
}


/**
 * Returns the connection to the pool of available connections.
 */
void
network_release_connection (struct connection * connection) {
#ifdef DEBUG
	printf("Releasing connection ... ");
#endif

	//
	// The connection pool must be protected by a lock so that multiple threads
	// releasing a connection do not corrupt the internal state of the pool.
	//
	pthread_mutex_lock (& disl_connections_mutex);
	{
		connection_pool_put_connection (& disl_connections, connection);
	}
	pthread_mutex_unlock (& disl_connections_mutex);

#ifdef DEBUG
	printf("done\n");
#endif
}
