const http = require('http');
http.createServer((req, res) => {
  if (req.url === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(`<html><script>
      fetch('/error').catch(e => {});
      fetch('/forbidden').catch(e => {});
      fetch('http://localhost:1/dead-port').catch(e => {});
    </script></html>`);
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
