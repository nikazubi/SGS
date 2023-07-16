import FormModal from "../../../components/modal/FormModal";
import {useState} from "react";
import useCreateStudents from "./useCreateStudents";
import {useFormikContext} from "formik";
import useUpdateStudent, {updateSubject} from "./useUpdateStudent";
import StudentForm from "./StudentForm";
import {ModalOpenMode} from "../../../utils/constants";


const initialValues = {
    firstName: "",
    lastName: "",
    personalNumber:'',
    username:'',
    password:''
};

const StudentModal = ({open, subject, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateStudents();
    const {mutate: onUpdate} = useUpdateStudent();

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={350}
            maxWidth={700}
            maxHeight={350}
            initialValues={

                {
                    ...initialValues,
                    ...subject,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={StudentForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default StudentModal;