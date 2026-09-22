const http = require('http');
http.createServer((req, res) => {
  if (req.url === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(`<html><script>
      fetch('/error').catch(e => {});
      fetch('/forbidden').catch(e => {});
      fetch('http://localhost:1/dead-port').catch(e => {});
    </script></html>`);
  } else if (req.url === '/routed') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(`<html><script>
      fetch('/api/echo', { method: 'POST', body: 'payload' })
        .then(r => r.text())
        .then(t => { document.title = 'done ' + t + ' seen=' + performance.getEntriesByType('resource').map(e => e.name).filter(n => n.includes('/api/')).join(','); });
    </script></html>`);
  } else if (req.url.startsWith('/api/')) {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('not routed');
  } else if (req.url === '/error') {
    res.writeHead(500, { 'Content-Type': 'text/plain' });
    res.end('Something went wrong: detailed error message from server');
  } else if (req.url === '/forbidden') {
    res.writeHead(403, { 'Content-Type': 'text/html' });
    res.end('<html><body><h1>Forbidden</h1><p>Your request was blocked by policy.</p></body></html>');
  } else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('OK');
  }
}).listen(3456, () => console.log('Test server on http://localhost:3456'));

http.createServer((req, res) => {
  let body = '';
  req.on('data', chunk => body += chunk);
  req.on('end', () => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end(`${req.method} ${req.url} body=${body}`);
  });
}).listen(3457, () => console.log('Echo server on http://localhost:3457'));
