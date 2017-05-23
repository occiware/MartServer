# Development status
## Travis CI
[![Build Status](https://travis-ci.org/occiware/MartServer.svg?branch=master)](https://travis-ci.org/occiware/MartServer)

Development Status values :
* TODO (Nothing has been done but will be done in near future)
* In progress (in development progress)
* Done (works and tested)
* Problem (done but not tested or have bugs)

## Tasks
<table>
    <th>Task name</th>
    <th>Comment (and related issues)</th>
    <th>Progress status</th>
    <tr>
        <td align="center">Create submodules</td>
        <td align="center">Integrate maven submodules server, servlet, jetty, war, occinterface, <a href="https://github.com/occiware/MartServer/issues/27">feature #1</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/done.png" alt="Done" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Add nanohttpd module and implementation</td>
        <td align="center">Add nanohttpd module, implementation must be done <a href="https://github.com/occiware/MartServer/issues/35">feature #13</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/todo.png" alt="TODO" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Add swagger ui module</td>
        <td align="center">Add swagger module,and add a feature to server swagger json output description, <a href="https://github.com/occiware/MartServer/issues/34">feature #14</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/todo.png" alt="TODO" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">War build</td>
        <td align="center">Add a module to build war for all project, <a href="https://github.com/occiware/MartServer/issues/28">feature #2</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/done.png" alt="Done" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Test application deployment</td>
        <td align="center">with Heroku or google app engine, <a href="https://github.com/occiware/MartServer/issues/28">feature #3</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/todo.png" alt="TODO" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Better management of resources locations path for web servlet engine</td>
        <td align="center">Related to issue #26 and #25, <a href="https://github.com/occiware/MartServer/issues/26">feature #4</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/done.png" alt="Done" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Refactor server engine</td>
        <td align="center">To abstract the use of configuration model with servlet engine, linked with submodules refactoring, see <a href="https://github.com/occiware/MartServer/issues/27">feature #5</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/done.png" alt="Done" height="40" width="auto" /></td>
        </tr>
    <tr>
        <td align="center">Update and refactor servlet engine</td>
        <td align="center">To fix issues about REST semantics (principaly PUT and POST methods, <a href="https://github.com/occiware/MartServer/issues/12">feature #6</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/done.png" alt="Done" height="40" width="auto" /></td>
    </tr>
    <tr>
            <td align="center">Give the ability to manage multiple connectors</td>
            <td align="center">There are a lot of works here because this is linked to refactoring of the model@runtime engine, <a href="https://github.com/occiware/MartServer/issues/16">feature #7</a></td>
            <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/todo.png" alt="TODO" height="40" width="auto" /></td>
        </tr>
    <tr>
        <td align="center">text/plain media type parser (Content-Type and accept)</td>
        <td align="center">Query parser to retrieve attributes etc. and output to client, <a href="https://github.com/occiware/MartServer/issues/7">feature #8</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/done.png" alt="Done" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Add https support with certificate</td>
        <td align="center">Https SSL support with X509 certificate, <a href="https://github.com/occiware/MartServer/issues/9">feature #9</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/inprogress.png" alt="In progress" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Add authentication token mechanism (OAUTH 2) support</td>
        <td align="center">Server authentication with tokens (refresh token and usage token), <a href="https://github.com/occiware/MartServer/issues/9">feature #10</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/inprogress.png" alt="In progress" height="40" width="auto" /></td>
    </tr>
    <tr>
        <td align="center">Add occinterface and swagger ui and rendering support</td>
        <td align="center">occinterface added but there is a problem with sever part, waiting about having a workaround with occinterface developers, swagger ui has not been included for now, <a href="https://github.com/occiware/MartServer/issues/11">feature #11</a></td>
        <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/problem.png" alt="problem" height="40" width="auto" /></td>
    </tr>
    <tr>
         <td align="center">Update user documentation</td>
         <td align="center">Add text/occi + text/plain documentation, update json documentation, <a href="https://github.com/occiware/MartServer/issues/33">feature #12</a></td>
         <td align="center"><img src="https://raw.github.com/occiware/MartServer/master/doc/inprogress.png" alt="In progress" height="40" width="auto" /></td>
    </tr>
    
</table>