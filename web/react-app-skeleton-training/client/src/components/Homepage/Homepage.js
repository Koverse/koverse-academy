import React, { useEffect, useState } from "react";
import axios from 'axios';
import { Button} from '@material-ui/core';
import { useNavigate } from "react-router-dom";

const Homepage = () => {

    const [userDisplayName, setUserDisplayName] = useState('');
    const [userEmail, setUserEmail] = useState('');

    // Data read from KDP4 stored here
    const [data, setData] = useState([]);
    const navigate = useNavigate();

    const logout = () => {
        // TRAINING: KDP4 Authentication 
        // Remove token and user from local storage so user is no longer recognized as logged in
        localStorage.removeItem('user');

        //call logout endpoint to update backend login status
        axios.get("/logout");
        navigate("/");
        window.location.reload();
    }

    // TRAINING: KDP4 Read Records from Datasets
    // Read data from KDP4 using the /getData query set in the server backend
    const getData = () => {
        axios.get('/getData')
        .then(response => {
            setData(response.data.records)
        })
        .catch(err => {
            console.log(err)
            console.log("DATA NOT RECEIVED")
        })
        
    }
    useEffect(() => {
        // sets jwt as a cookie and returns user credentials
        axios.get("/getCredentials") 
        .then(response => {
            // RECEIVED CREDENTIALS
            localStorage.setItem("user", JSON.stringify(response.data))
            setUserDisplayName(response.data.displayName);        
            setUserEmail(response.data.email); 
        })
        .catch(err => {
            // UNABLE TO GET USER CREDENTIALS
            logout();
            navigate("/");
            window.location.reload();
        })
    }, [])

    return (
        <div>
      
                <>
                    <p>Homepage</p>
                    <b>User Credentials: </b>
                    <p>{userDisplayName}</p>
                    <p>{userEmail}</p>
                    <Button style={{color: 'white', background: 'gray'}}
                    onClick={()=> {logout()}}
                    >Log-out</Button>
                    <Button style={{color: 'white', background: 'gray'}}
                    onClick={()=> {getData()}}
                    >Get Data</Button>
                    {data.length !== 0 && 
                        <div>{JSON.stringify(data)}</div>
                    }
                </> 
            
        </div>
        
    );

};


  export default Homepage;